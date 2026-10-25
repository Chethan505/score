package com.example.football.match;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface SeasonRepository extends JpaRepository<Season, UUID> {

    Optional<Season> findByProviderSeasonId(String providerSeasonId);

    List<Season> findByCompetitionId(UUID competitionId);

    Optional<Season> findByCompetitionIdAndIsCurrentTrue(UUID competitionId);

    boolean existsByProviderSeasonId(String providerSeasonId);
}