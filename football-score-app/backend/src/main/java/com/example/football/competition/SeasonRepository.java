package com.example.football.competition;

import com.example.football.match.Season;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface SeasonRepository extends JpaRepository<Season, UUID> {

    Optional<Season> findByProviderSeasonId(String providerSeasonId);

    List<Season> findByCompetitionId(UUID competitionId);

    Optional<Season> findByCompetitionIdAndIsCurrentTrue(UUID competitionId);

    boolean existsByProviderSeasonId(String providerSeasonId);
}
