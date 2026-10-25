package com.example.football.notification;

import com.example.football.auth.User;
import com.example.football.event.MatchEvent;
import com.example.football.match.Match;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.OffsetDateTime;
import java.util.UUID;

@Entity
@Table(name = "notifications", indexes = {
        @Index(name = "idx_notifications_user_status", columnList = "user_id, status"),
        @Index(name = "idx_notifications_scheduled", columnList = "scheduled_for, status", where = "status = 'PENDING'"),
        @Index(name = "idx_notifications_match", columnList = "match_id")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Notification {

    @Id
    @GeneratedValue
    @Column(name = "id", updatable = false, nullable = false)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "match_id")
    private Match match;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "event_id")
    private MatchEvent event;

    @Column(name = "type", nullable = false, length = 30)
    private String type;

    @Column(name = "template_name", length = 100)
    private String templateName;

    @Column(name = "status", nullable = false, length = 20)
    private String status = "PENDING";

    @Column(name = "whatsapp_message_id", length = 128)
    private String whatsappMessageId;

    @Column(name = "attempt_count", nullable = false)
    private Integer attemptCount = 0;

    @Column(name = "last_error", columnDefinition = "TEXT")
    private String lastError;

    @Column(name = "scheduled_for")
    private OffsetDateTime scheduledFor;

    @Column(name = "sent_at")
    private OffsetDateTime sentAt;

    @Column(name = "delivered_at")
    private OffsetDateTime deliveredAt;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private OffsetDateTime createdAt;
}