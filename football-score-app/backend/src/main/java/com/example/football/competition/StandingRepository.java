package com.example.football.competition;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface StandingRepository extends JpaRepository<Standing, UUID> {

    List<Standing> findByCompetitionIdAndSeasonIdOrderByPositionAsc(UUID competitionId, UUID seasonId);

    Optional<Standing> findByCompetitionIdAndSeasonIdAndTeamId(UUID competitionId, UUID seasonId, UUID teamId);

    @Query("SELECT s FROM Standing s WHERE s.competition.id = :compId AND s.season.id = :seasonId ORDER BY s.position ASC")
    List<Standing> findByCompetitionAndSeason(@Param("compId") UUID compId, @Param("seasonId") UUID seasonId);
}