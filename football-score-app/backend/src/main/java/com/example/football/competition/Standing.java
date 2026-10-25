package com.example.football.competition;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.OffsetDateTime;
import java.util.UUID;

@Entity
@Table(name = "standings", indexes = {
        @Index(name = "idx_standings_comp_season", columnList = "competition_id, season_id, position"),
        @Index(name = "idx_standings_team", columnList = "team_id"),
        @Index(name = "idx_standings_unique", columnList = "competition_id, season_id, team_id", unique = true)
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Standing {

    @Id
    @GeneratedValue
    @Column(name = "id", updatable = false, nullable = false)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "competition_id", nullable = false)
    private Competition competition;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "season_id", nullable = false)
    private Season season;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "team_id", nullable = false)
    private Team team;

    @Column(name = "position", nullable = false)
    private Integer position;

    @Column(name = "played", nullable = false)
    private Integer played = 0;

    @Column(name = "won", nullable = false)
    private Integer won = 0;

    @Column(name = "draw", nullable = false)
    private Integer draw = 0;

    @Column(name = "lost", nullable = false)
    private Integer lost = 0;

    @Column(name = "goals_for", nullable = false)
    private Integer goalsFor = 0;

    @Column(name = "goals_against", nullable = false)
    private Integer goalsAgainst = 0;

    @Column(name = "goal_difference", nullable = false)
    private Integer goalDifference = 0;

    @Column(name = "points", nullable = false)
    private Integer points = 0;

    @Column(name = "form", length = 20)
    private String form;

    @Column(name = "last_updated", nullable = false)
    private OffsetDateTime lastUpdated = OffsetDateTime.now();

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private OffsetDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private OffsetDateTime updatedAt;
}