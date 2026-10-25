package com.example.football.event;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.OffsetDateTime;
import java.util.UUID;

@Entity
@Table(name = "match_events", indexes = {
        @Index(name = "idx_match_events_match", columnList = "match_id, occurred_at"),
        @Index(name = "idx_match_events_type", columnList = "event_type"),
        @Index(name = "idx_match_events_provider", columnList = "match_id, provider_event_id", unique = true)
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class MatchEvent {

    @Id
    @GeneratedValue
    @Column(name = "id", updatable = false, nullable = false)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "match_id", nullable = false)
    private Match match;

    @Column(name = "provider_event_id", length = 50)
    private String providerEventId;

    @Column(name = "event_type", nullable = false, length = 30)
    private String eventType;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "team_id")
    private Team team;

    @Column(name = "player_id")
    private UUID playerId;

    @Column(name = "player_name", length = 100)
    private String playerName;

    @Column(name = "minute")
    private Integer minute;

    @Column(name = "extra_minute")
    private Integer extraMinute;

    @Column(name = "description", columnDefinition = "TEXT")
    private String description;

    @Column(name = "metadata", columnDefinition = "JSONB")
    private String metadata;

    @Column(name = "occurred_at", nullable = false)
    private OffsetDateTime occurredAt;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private OffsetDateTime createdAt;

    public String getEventKey() {
        return match.getId() + ":" + eventType + ":" + providerEventId;
    }
}