package com.example.football.notification;

import com.example.football.auth.User;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.OffsetDateTime;
import java.util.UUID;

@Entity
@Table(name = "notification_preferences", indexes = {
        @Index(name = "idx_notif_prefs_user", columnList = "user_id", unique = true)
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class NotificationPreference {

    @Id
    @GeneratedValue
    @Column(name = "id", updatable = false, nullable = false)
    private UUID id;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false, unique = true)
    private User user;

    @Column(name = "match_start_reminder", nullable = false)
    private Boolean matchStartReminder = true;

    @Column(name = "goal_notifications", nullable = false)
    private Boolean goalNotifications = true;

    @Column(name = "card_notifications", nullable = false)
    private Boolean cardNotifications = false;

    @Column(name = "substitution_notifications", nullable = false)
    private Boolean substitutionNotifications = false;

    @Column(name = "half_time_notifications", nullable = false)
    private Boolean halfTimeNotifications = false;

    @Column(name = "full_time_notifications", nullable = false)
    private Boolean fullTimeNotifications = true;

    @Column(name = "reminder_minutes_before", nullable = false)
    private Integer reminderMinutesBefore = 15;

    @Column(name = "quiet_hours_start")
    private java.time.LocalTime quietHoursStart;

    @Column(name = "quiet_hours_end")
    private java.time.LocalTime quietHoursEnd;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private OffsetDateTime updatedAt;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private OffsetDateTime createdAt;
}