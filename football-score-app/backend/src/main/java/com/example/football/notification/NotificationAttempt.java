package com.example.football.notification;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.OffsetDateTime;
import java.util.UUID;

@Entity
@Table(name = "notification_attempts", indexes = {
        @Index(name = "idx_notif_attempts_notification", columnList = "notification_id")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class NotificationAttempt {

    @Id
    @GeneratedValue
    @Column(name = "id", updatable = false, nullable = false)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "notification_id", nullable = false)
    private Notification notification;

    @Column(name = "attempt_number", nullable = false)
    private Integer attemptNumber;

    @Column(name = "status", nullable = false, length = 20)
    private String status;

    @Column(name = "error_code", length = 50)
    private String errorCode;

    @Column(name = "error_message", columnDefinition = "TEXT")
    private String errorMessage;

    @Column(name = "response_body", columnDefinition = "JSONB")
    private String responseBody;

    @CreationTimestamp
    @Column(name = "attempted_at", nullable = false, updatable = false)
    private OffsetDateTime attemptedAt;
}