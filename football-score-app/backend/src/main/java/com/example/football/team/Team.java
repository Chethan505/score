package com.example.football.team;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.OffsetDateTime;
import java.util.UUID;

@Entity
@Table(name = "teams", indexes = {
        @Index(name = "idx_teams_provider_id", columnList = "provider_team_id", unique = true),
        @Index(name = "idx_teams_competition", columnList = "competition_id")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Team {

    @Id
    @GeneratedValue
    @Column(name = "id", updatable = false, nullable = false)
    private UUID id;

    @Column(name = "provider_team_id", unique = true, nullable = false, length = 50)
    private String providerTeamId;

    @Column(name = "name", nullable = false, length = 100)
    private String name;

    @Column(name = "short_name", length = 20)
    private String shortName;

    @Column(name = "logo_url", length = 500)
    private String logoUrl;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "competition_id")
    private Competition competition;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private OffsetDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private OffsetDateTime updatedAt;
}