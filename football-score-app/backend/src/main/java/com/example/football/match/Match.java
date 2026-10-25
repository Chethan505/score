package com.example.football.match;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.OffsetDateTime;
import java.util.UUID;

@Entity
@Table(name = "matches", indexes = {
        @Index(name = "idx_matches_status_scheduled", columnList = "status, scheduled_start"),
        @Index(name = "idx_matches_competition_date", columnList = "competition_id, scheduled_start"),
        @Index(name = "idx_matches_teams", columnList = "home_team_id, away_team_id")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Match {

    @Id
    @GeneratedValue
    @Column(name = "id", updatable = false, nullable = false)
    private UUID id;

    @Column(name = "provider_match_id", unique = true, nullable = false, length = 50)
    private String providerMatchId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "competition_id", nullable = false)
    private Competition competition;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "season_id")
    private Season season;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "home_team_id", nullable = false)
    private Team homeTeam;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "away_team_id", nullable = false)
    private Team awayTeam;

    @Column(name = "scheduled_start", nullable = false)
    private OffsetDateTime scheduledStart;

    @Column(name = "status", nullable = false, length = 30)
    private String status;

    @Column(name = "home_score", nullable = false)
    private Integer homeScore = 0;

    @Column(name = "away_score", nullable = false)
    private Integer awayScore = 0;

    @Column(name = "match_minute")
    private Integer matchMinute;

    @Column(name = "extra_minute")
    private Integer extraMinute;

    @Column(name = "last_provider_update")
    private OffsetDateTime lastProviderUpdate;

    @Version
    @Column(name = "version", nullable = false)
    private Integer version = 0;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private OffsetDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private OffsetDateTime updatedAt;

    public boolean isLive() {
        return "LIVE".equals(status) || "HALFTIME".equals(status) || "EXTRA_TIME".equals(status);
    }

    public boolean isFinished() {
        return "FINISHED".equals(status) || "POSTPONED".equals(status) || "CANCELLED".equals(status) || "ABANDONED".equals(status);
    }
}