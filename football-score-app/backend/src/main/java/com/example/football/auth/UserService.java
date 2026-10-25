package com.example.football.auth;

import com.example.football.notification.NotificationPreference;
import com.example.football.notification.NotificationPreferenceRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;
import java.util.UUID;

@Service
@Transactional
public class UserService {

    private final UserRepository userRepository;
    private final NotificationPreferenceRepository preferenceRepository;

    public UserService(UserRepository userRepository, NotificationPreferenceRepository preferenceRepository) {
        this.userRepository = userRepository;
        this.preferenceRepository = preferenceRepository;
    }

    public User findOrCreateByPhoneNumber(String phoneNumber) {
        return userRepository.findByPhoneNumber(phoneNumber)
                .orElseGet(() -> createUser(phoneNumber));
    }

    private User createUser(String phoneNumber) {
        User user = User.builder()
                .phoneNumber(phoneNumber)
                .whatsappOptIn(false)
                .timezone("Asia/Kolkata")
                .build();
        User saved = userRepository.save(user);

        NotificationPreference prefs = NotificationPreference.builder()
                .user(saved)
                .matchStartReminder(true)
                .goalNotifications(true)
                .cardNotifications(false)
                .substitutionNotifications(false)
                .halfTimeNotifications(false)
                .fullTimeNotifications(true)
                .reminderMinutesBefore(15)
                .build();
        preferenceRepository.save(prefs);

        return saved;
    }

    public User getUser(UUID userId) {
        return userRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("User not found: " + userId));
    }

    public User updateOptIn(UUID userId, boolean optIn) {
        User user = getUser(userId);
        user.setWhatsappOptIn(optIn);
        if (optIn) {
            user.setOptInAt(java.time.OffsetDateTime.now());
        }
        return userRepository.save(user);
    }

    public User updateTimezone(UUID userId, String timezone) {
        User user = getUser(userId);
        user.setTimezone(timezone);
        return userRepository.save(user);
    }

    public Optional<NotificationPreference> getPreferences(UUID userId) {
        return preferenceRepository.findByUserId(userId);
    }

    public NotificationPreference updatePreferences(UUID userId, NotificationPreference preferences) {
        NotificationPreference existing = preferenceRepository.findByUserId(userId)
                .orElseGet(() -> {
                    NotificationPreference newPrefs = NotificationPreference.builder()
                            .user(getUser(userId))
                            .build();
                    return preferenceRepository.save(newPrefs);
                });

        existing.setMatchStartReminder(preferences.getMatchStartReminder());
        existing.setGoalNotifications(preferences.getGoalNotifications());
        existing.setCardNotifications(preferences.getCardNotifications());
        existing.setSubstitutionNotifications(preferences.getSubstitutionNotifications());
        existing.setHalfTimeNotifications(preferences.getHalfTimeNotifications());
        existing.setFullTimeNotifications(preferences.getFullTimeNotifications());
        existing.setReminderMinutesBefore(preferences.getReminderMinutesBefore());
        existing.setQuietHoursStart(preferences.getQuietHoursStart());
        existing.setQuietHoursEnd(preferences.getQuietHoursEnd());

        return preferenceRepository.save(existing);
    }
}