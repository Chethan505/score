package com.example.football.notification;

import com.example.football.auth.User;
import com.example.football.auth.UserRepository;
import com.example.football.event.MatchEvent;
import com.example.football.event.MatchEventRepository;
import com.example.football.match.Match;
import com.example.football.match.MatchRepository;
import com.example.football.whatsapp.WhatsAppService;
import com.example.football.whatsapp.dto.WhatsAppTemplateMessage;
import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import io.github.resilience4j.ratelimiter.annotation.RateLimiter;
import io.github.resilience4j.retry.annotation.Retry;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.connection.stream.Consumer;
import org.springframework.data.redis.connection.stream.MapRecord;
import org.springframework.data.redis.connection.stream.ReadOffset;
import org.springframework.data.redis.connection.stream.StreamOffset;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Service
public class NotificationWorker {

    private static final Logger log = LoggerFactory.getLogger(NotificationWorker.class);

    private final NotificationRepository notificationRepository;
    private final NotificationAttemptRepository attemptRepository;
    private final WhatsAppService whatsAppService;
    private final RedisTemplate<String, Object> redisTemplate;
    private final UserRepository userRepository;
    private final MatchRepository matchRepository;
    private final MatchEventRepository matchEventRepository;
    private final NotificationQueueService queueService;

    @Value("${notification.queue.name:notification:queue}")
    private String queueName;

    @Value("${notification.queue.consumer-group:notification-workers}")
    private String consumerGroup;

    @Value("${notification.worker.concurrency:5}")
    private int concurrency;

    @Value("${whatsapp.retry.max-attempts:4}")
    private int maxAttempts;

    @Value("${whatsapp.retry.base-delay-ms:5000}")
    private long baseDelayMs;

    @Value("${whatsapp.retry.max-delay-ms:60000}")
    private long maxDelayMs;

    @Value("${scheduler.match-start-reminder-minutes:15}")
    private int matchStartReminderMinutes;

    public NotificationWorker(
            NotificationRepository notificationRepository,
            NotificationAttemptRepository attemptRepository,
            WhatsAppService whatsAppService,
            RedisTemplate<String, Object> redisTemplate,
            UserRepository userRepository,
            MatchRepository matchRepository,
            MatchEventRepository matchEventRepository,
            NotificationQueueService queueService
    ) {
        this.notificationRepository = notificationRepository;
        this.attemptRepository = attemptRepository;
        this.whatsAppService = whatsAppService;
        this.redisTemplate = redisTemplate;
        this.userRepository = userRepository;
        this.matchRepository = matchRepository;
        this.matchEventRepository = matchEventRepository;
        this.queueService = queueService;
    }

    @Scheduled(fixedDelay = 1000)
    @Transactional
    public void processQueue() {
        try {
            List<MapRecord<String, Object, Object>> records = redisTemplate.opsForStream()
                    .read(
                            Consumer.from(consumerGroup, "worker-" + Thread.currentThread().getId()),
                            StreamOffset.create(queueName, ReadOffset.lastConsumed()),
                            concurrency
                    );

            if (records == null || records.isEmpty()) {
                return;
            }

            for (MapRecord<String, Object, Object> record : records) {
                processRecord(record);
                redisTemplate.opsForStream().acknowledge(queueName, consumerGroup, record.getId());
            }
        } catch (Exception e) {
            log.error("Error processing notification queue", e);
        }
    }

    private void processRecord(MapRecord<String, Object, Object> record) {
        String notificationIdStr = (String) record.getValue().get("notificationId");
        if (notificationIdStr == null) {
            log.warn("Invalid notification record: {}", record.getId());
            return;
        }

        UUID notificationId = UUID.fromString(notificationIdStr);
        Notification notification = notificationRepository.findById(notificationId).orElse(null);

        if (notification == null) {
            log.warn("Notification not found: {}", notificationId);
            return;
        }

        if (!"PENDING".equals(notification.getStatus()) && !"RETRYING".equals(notification.getStatus())) {
            log.debug("Notification {} already processed with status: {}", notificationId, notification.getStatus());
            return;
        }

        sendNotification(notification);
    }

    @Retry(name = "whatsapp", fallbackMethod = "sendNotificationFallback")
    @CircuitBreaker(name = "whatsapp")
    @RateLimiter(name = "whatsapp")
    private void sendNotification(Notification notification) {
        User user = userRepository.findById(notification.getUser().getId()).orElse(null);
        if (user == null || !user.getWhatsappOptIn()) {
            markAsFailed(notification, "User not found or not opted in");
            return;
        }

        Match match = matchRepository.findById(notification.getMatch().getId()).orElse(null);
        if (match == null) {
            markAsFailed(notification, "Match not found");
            return;
        }

        MatchEvent event = null;
        if (notification.getEvent() != null) {
            event = matchEventRepository.findById(notification.getEvent().getId()).orElse(null);
        }

        WhatsAppTemplateMessage templateMessage = buildTemplateMessage(notification, user, match, event);
        String messageId = whatsAppService.sendTemplateMessage(templateMessage);

        notification.setStatus("SENT");
        notification.setSentAt(OffsetDateTime.now());
        notification.setWhatsappMessageId(messageId);
        notificationRepository.save(notification);

        recordAttempt(notification, notification.getAttemptCount() + 1, "SENT", null, null, null);
    }

    private void sendNotificationFallback(Notification notification, Exception ex) {
        log.error("Failed to send notification {} after retries", notification.getId(), ex);
        markAsFailed(notification, ex.getMessage());
    }

    private void markAsFailed(Notification notification, String error) {
        int attemptCount = notification.getAttemptCount() + 1;
        notification.setAttemptCount(attemptCount);
        notification.setLastError(error);

        if (attemptCount >= maxAttempts) {
            notification.setStatus("DEAD_LETTER");
            log.error("Notification {} moved to dead letter queue after {} attempts", 
                    notification.getId(), maxAttempts);
        } else {
            notification.setStatus("RETRYING");
            long delay = calculateBackoff(attemptCount);
            notification.setScheduledFor(OffsetDateTime.now().plusMillis(delay));
            scheduleRetry(notification, delay);
        }

        notificationRepository.save(notification);
        recordAttempt(notification, attemptCount, "FAILED", null, error, null);
    }

    long calculateBackoff(int attemptNumber) {
        long delay = baseDelayMs * (long) Math.pow(2, attemptNumber - 1);
        return Math.min(delay, maxDelayMs);
    }

    int getMaxAttempts() {
        return maxAttempts;
    }

    long getBaseDelayMs() {
        return baseDelayMs;
    }

    long getMaxDelayMs() {
        return maxDelayMs;
    }

    private void scheduleRetry(Notification notification, long delayMs) {
        // Schedule retry via Redis delayed queue or separate scheduler
        // For simplicity, we'll use a scheduled task approach
        new Thread(() -> {
            try {
                Thread.sleep(delayMs);
                queueService.enqueue(notification.getId());
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                log.warn("Retry scheduling interrupted for notification {}", notification.getId());
            }
        }).start();
    }

    private void recordAttempt(Notification notification, int attemptNumber, String status, 
                               String errorCode, String errorMessage, String responseBody) {
        NotificationAttempt attempt = NotificationAttempt.builder()
                .notification(notification)
                .attemptNumber(attemptNumber)
                .status(status)
                .errorCode(errorCode)
                .errorMessage(errorMessage)
                .responseBody(responseBody)
                .build();
        attemptRepository.save(attempt);
    }

    private WhatsAppTemplateMessage buildTemplateMessage(
            Notification notification,
            User user,
            Match match,
            MatchEvent event
    ) {
        String templateName = notification.getTemplateName();
        Map<String, String> variables = buildTemplateVariables(notification, match, event);

        return WhatsAppTemplateMessage.builder()
                .to(user.getPhoneNumber())
                .templateName(templateName)
                .languageCode("en")
                .variables(variables)
                .build();
    }

    private Map<String, String> buildTemplateVariables(
            Notification notification,
            Match match,
            MatchEvent event
    ) {
        return Map.of(
                "team_home", match.getHomeTeam().getName(),
                "team_away", match.getAwayTeam().getName(),
                "league", match.getCompetition().getName(),
                "score_home", String.valueOf(match.getHomeScore()),
                "score_away", String.valueOf(match.getAwayScore()),
                "minute", match.getMatchMinute() != null ? String.valueOf(match.getMatchMinute()) : "0",
                "team_scoring", event != null && event.getTeam() != null ? event.getTeam().getName() : "",
                "event_icon", getEventIcon(notification.getType()),
                "event_type", getEventTypeLabel(notification.getType()),
                "minutes", String.valueOf(matchStartReminderMinutes)
        );
    }

    private String getEventIcon(String type) {
        return switch (type) {
            case "GOAL" -> "⚽";
            case "YELLOW_CARD" -> "🟨";
            case "RED_CARD" -> "🟥";
            case "SUBSTITUTION" -> "🔄";
            case "HALFTIME" -> "🍊";
            case "FULLTIME" -> "🏁";
            case "MATCH_START" -> "⚽";
            default -> "📢";
        };
    }

    private String getEventTypeLabel(String type) {
        return switch (type) {
            case "GOAL" -> "Goal";
            case "YELLOW_CARD" -> "Yellow Card";
            case "RED_CARD" -> "Red Card";
            case "SUBSTITUTION" -> "Substitution";
            case "HALFTIME" -> "Half Time";
            case "FULLTIME" -> "Full Time";
            case "MATCH_START" -> "Match Starting";
            default -> "Event";
        };
    }
}