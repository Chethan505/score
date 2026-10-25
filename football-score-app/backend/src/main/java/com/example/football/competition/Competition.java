package com.example.football.competition;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.OffsetDateTime;
import java.util.UUID;

@Entity
@Table(name = "competitions", indexes = {
        @Index(name = "idx_competitions_provider_id", columnList = "provider_competition_id", unique = true)
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Competition {

    @Id
    @GeneratedValue
    @Column(name = "id", updatable = false, nullable = false)
    private UUID id;

    @Column(name = "provider_competition_id", unique = true, nullable = false, length = 50)
    private String providerCompetitionId;

    @Column(name = "name", nullable = false, length = 100)
    private String name;

    @Column(name = "country", length = 50)
    private String country;

    @Column(name = "logo_url", length = 500)
    private String logoUrl;

    @Column(name = "is_active", nullable = false)
    private Boolean isActive = true;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private OffsetDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private OffsetDateTime updatedAt;
}