package com.example.football.match;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface MatchRepository extends JpaRepository<Match, UUID> {

    Optional<Match> findByProviderMatchId(String providerMatchId);

    List<Match> findByStatusInAndScheduledStartBetween(
            List<String> statuses,
            OffsetDateTime from,
            OffsetDateTime to
    );

    List<Match> findByStatusIn(List<String> statuses);

    List<Match> findByCompetitionIdAndScheduledStartBetween(
            UUID competitionId,
            OffsetDateTime from,
            OffsetDateTime to
    );

    @Query("SELECT m FROM Match m WHERE m.status IN :statuses AND m.scheduledStart <= :now")
    List<Match> findLiveAndUpcomingMatches(@Param("statuses") List<String> statuses, @Param("now") OffsetDateTime now);

    @Query("SELECT m FROM Match m WHERE m.status = 'SCHEDULED' AND m.scheduledStart BETWEEN :from AND :to")
    List<Match> findUpcomingMatches(@Param("from") OffsetDateTime from, @Param("to") OffsetDateTime to);

    @Query("SELECT m FROM Match m WHERE m.status IN ('LIVE', 'HALFTIME', 'EXTRA_TIME')")
    List<Match> findCurrentlyLiveMatches();

    @Query("SELECT m FROM Match m WHERE m.status = 'SCHEDULED' AND m.scheduledStart <= :now")
    List<Match> findMatchesStartingSoon(@Param("now") OffsetDateTime now);

    boolean existsByProviderMatchId(String providerMatchId);
}