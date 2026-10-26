package com.example.football.notification;

import com.example.football.auth.User;
import com.example.football.event.MatchEvent;
import com.example.football.favorite.FavoriteMatch;
import com.example.football.favorite.FavoriteMatchRepository;
import com.example.football.favorite.FavoriteTeam;
import com.example.football.favorite.FavoriteTeamRepository;
import com.example.football.match.Match;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@Transactional
public class NotificationService {

    private final NotificationRepository notificationRepository;
    private final NotificationAttemptRepository attemptRepository;
    private final FavoriteMatchRepository favoriteMatchRepository;
    private final FavoriteTeamRepository favoriteTeamRepository;
    private final NotificationQueueService queueService;

    @Value("${scheduler.match-start-reminder-minutes:15}")
    private int matchStartReminderMinutes;

    public NotificationService(
            NotificationRepository notificationRepository,
            NotificationAttemptRepository attemptRepository,
            FavoriteMatchRepository favoriteMatchRepository,
            FavoriteTeamRepository favoriteTeamRepository,
            NotificationQueueService queueService
    ) {
        this.notificationRepository = notificationRepository;
        this.attemptRepository = attemptRepository;
        this.favoriteMatchRepository = favoriteMatchRepository;
        this.favoriteTeamRepository = favoriteTeamRepository;
        this.queueService = queueService;
    }

    public void createMatchStartReminder(Match match, int minutesBefore) {
        Set<User> users = getUsersToNotify(match);
        String templateName = "match_starting_soon";

        for (User user : users) {
            if (shouldNotifyUser(user, "match_start_reminder")) {
                createNotification(user, match, null, "MATCH_START", templateName, 
                        OffsetDateTime.now(), minutesBefore);
            }
        }
    }

    public void createMatchStartNotification(Match match) {
        Set<User> users = getUsersToNotify(match);
        String templateName = "match_event";

        for (User user : users) {
            if (shouldNotifyUser(user, "match_start_reminder")) {
                createNotification(user, match, null, "MATCH_START", templateName, 
                        OffsetDateTime.now(), 0);
            }
        }
    }

    public void createGoalNotification(Match match, com.example.football.footballprovider.model.MatchEvent goal, boolean homeTeamScored, int homeScore, int awayScore) {
        Set<User> users = getUsersToNotify(match);

        for (User user : users) {
            if (shouldNotifyUser(user, "goal_notifications")) {
                Notification notification = createNotification(user, match, null, "GOAL", "goal_notification", 
                        OffsetDateTime.now(), 0);
            }
        }
    }

    public void createHalfTimeNotification(Match match) {
        Set<User> users = getUsersToNotify(match);

        for (User user : users) {
            if (shouldNotifyUser(user, "half_time_notifications")) {
                createNotification(user, match, null, "HALFTIME", "match_event", 
                        OffsetDateTime.now(), 0);
            }
        }
    }

    public void createFullTimeNotification(Match match) {
        Set<User> users = getUsersToNotify(match);

        for (User user : users) {
            if (shouldNotifyUser(user, "full_time_notifications")) {
                createNotification(user, match, null, "FULLTIME", "full_time_result", 
                        OffsetDateTime.now(), 0);
            }
        }
    }

    public void createPostponedCancelledNotification(Match match, String status) {
        Set<User> users = getUsersToNotify(match);

        for (User user : users) {
            createNotification(user, match, null, status, "match_event", 
                    OffsetDateTime.now(), 0);
        }
    }

    public void createEventNotification(Match match, com.example.football.footballprovider.model.MatchEvent event, String templateName) {
        Set<User> users = getUsersToNotify(match, event);

        for (User user : users) {
            boolean shouldNotify = switch (event.type()) {
                case "YELLOW_CARD", "RED_CARD" -> shouldNotifyUser(user, "card_notifications");
                case "SUBSTITUTION" -> shouldNotifyUser(user, "substitution_notifications");
                default -> true;
            };

            if (shouldNotify) {
                createNotification(user, match, null, event.type().toUpperCase(), templateName, 
                        OffsetDateTime.now(), 0);
            }
        }
    }

    private Notification createNotification(
            User user,
            Match match,
            MatchEvent event,
            String type,
            String templateName,
            OffsetDateTime scheduledFor,
            int minutesBefore
    ) {
        boolean exists = notificationRepository.findByMatchIdAndType(match.getId(), type)
                .stream()
                .anyMatch(n -> n.getUser().getId().equals(user.getId()) 
                        && ("PENDING".equals(n.getStatus()) || "SENT".equals(n.getStatus()) || "DELIVERED".equals(n.getStatus())));

        if (exists) {
            return null;
        }

        Notification notification = Notification.builder()
                .user(user)
                .match(match)
                .event(event)
                .type(type)
                .templateName(templateName)
                .status("PENDING")
                .scheduledFor(scheduledFor)
                .build();

        Notification saved = notificationRepository.save(notification);
        queueService.enqueue(saved.getId());
        return saved;
    }

    private Set<User> getUsersToNotify(Match match) {
        Set<User> users = new java.util.HashSet<>();

        List<FavoriteMatch> favoriteMatches = favoriteMatchRepository.findByMatchId(match.getId());
        users.addAll(favoriteMatches.stream().map(FavoriteMatch::getUser).collect(Collectors.toSet()));

        List<FavoriteTeam> favoriteHomeTeams = favoriteTeamRepository.findByTeamId(match.getHomeTeam().getId());
        users.addAll(favoriteHomeTeams.stream().map(FavoriteTeam::getUser).collect(Collectors.toSet()));

        List<FavoriteTeam> favoriteAwayTeams = favoriteTeamRepository.findByTeamId(match.getAwayTeam().getId());
        users.addAll(favoriteAwayTeams.stream().map(FavoriteTeam::getUser).collect(Collectors.toSet()));

        return users;
    }

    private Set<User> getUsersToNotify(Match match, com.example.football.footballprovider.model.MatchEvent event) {
        Set<User> users = getUsersToNotify(match);

        if (event.teamProviderId() != null) {
            List<FavoriteTeam> favoriteEventTeams = favoriteTeamRepository.findByTeamProviderId(event.teamProviderId());
            users.addAll(favoriteEventTeams.stream().map(FavoriteTeam::getUser).collect(Collectors.toSet()));
        }

        return users;
    }

    private boolean shouldNotifyUser(User user, String preferenceType) {
        return notificationRepository.findByUserId(user.getId())
                .map(prefs -> {
                    return switch (preferenceType) {
                        case "match_start_reminder" -> prefs.getMatchStartReminder();
                        case "goal_notifications" -> prefs.getGoalNotifications();
                        case "card_notifications" -> prefs.getCardNotifications();
                        case "substitution_notifications" -> prefs.getSubstitutionNotifications();
                        case "half_time_notifications" -> prefs.getHalfTimeNotifications();
                        case "full_time_notifications" -> prefs.getFullTimeNotifications();
                        default -> true;
                    };
                })
                .orElse(true);
    }
}