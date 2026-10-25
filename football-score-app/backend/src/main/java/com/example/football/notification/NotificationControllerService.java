package com.example.football.notification;

import com.example.football.auth.User;
import com.example.football.auth.UserRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@Transactional(readOnly = true)
public class NotificationControllerService {

    private final NotificationRepository notificationRepository;
    private final NotificationPreferenceRepository preferenceRepository;
    private final UserRepository userRepository;

    public NotificationControllerService(
            NotificationRepository notificationRepository,
            NotificationPreferenceRepository preferenceRepository,
            UserRepository userRepository
    ) {
        this.notificationRepository = notificationRepository;
        this.preferenceRepository = preferenceRepository;
        this.userRepository = userRepository;
    }

    public List<NotificationDto> getNotifications(UUID userId) {
        return notificationRepository.findByUserIdAndStatusIn(userId, List.of("SENT", "DELIVERED", "FAILED", "DEAD_LETTER")).stream()
                .map(this::mapToDto)
                .collect(Collectors.toList());
    }

    public NotificationPreferenceDto getPreferences(UUID userId) {
        return preferenceRepository.findByUserId(userId)
                .map(this::mapToDto)
                .orElseGet(() -> createDefaultPreferences(userId));
    }

    @Transactional
    public NotificationPreferenceDto updatePreferences(UUID userId, NotificationPreferenceDto preferences) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("User not found"));

        NotificationPreference existing = preferenceRepository.findByUserId(userId)
                .orElseGet(() -> {
                    NotificationPreference newPrefs = NotificationPreference.builder()
                            .user(user)
                            .build();
                    return preferenceRepository.save(newPrefs);
                });

        existing.setMatchStartReminder(preferences.matchStartReminder());
        existing.setGoalNotifications(preferences.goalNotifications());
        existing.setCardNotifications(preferences.cardNotifications());
        existing.setSubstitutionNotifications(preferences.substitutionNotifications());
        existing.setHalfTimeNotifications(preferences.halfTimeNotifications());
        existing.setFullTimeNotifications(preferences.fullTimeNotifications());
        existing.setReminderMinutesBefore(preferences.reminderMinutesBefore());
        existing.setQuietHoursStart(preferences.quietHoursStart());
        existing.setQuietHoursEnd(preferences.quietHoursEnd());

        NotificationPreference saved = preferenceRepository.save(existing);
        return mapToDto(saved);
    }

    private NotificationPreferenceDto createDefaultPreferences(UUID userId) {
        return new NotificationPreferenceDto(
                true, true, false, false, false, true, 15, null, null
        );
    }

    private NotificationDto mapToDto(Notification notification) {
        return new NotificationDto(
                notification.getId(),
                notification.getMatch() != null ? notification.getMatch().getId() : null,
                notification.getType(),
                notification.getTemplateName(),
                notification.getStatus(),
                notification.getScheduledFor(),
                notification.getSentAt(),
                notification.getDeliveredAt(),
                notification.getCreatedAt()
        );
    }

    private NotificationPreferenceDto mapToDto(NotificationPreference prefs) {
        return new NotificationPreferenceDto(
                prefs.getMatchStartReminder(),
                prefs.getGoalNotifications(),
                prefs.getCardNotifications(),
                prefs.getSubstitutionNotifications(),
                prefs.getHalfTimeNotifications(),
                prefs.getFullTimeNotifications(),
                prefs.getReminderMinutesBefore(),
                prefs.getQuietHoursStart(),
                prefs.getQuietHoursEnd()
        );
    }
}