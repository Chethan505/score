package com.example.football.event;

import com.example.football.match.Match;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface MatchEventRepository extends JpaRepository<MatchEvent, UUID> {

    List<MatchEvent> findByMatchIdOrderByOccurredAtAsc(UUID matchId);

    List<MatchEvent> findByMatchIdAndEventType(UUID matchId, String eventType);

    Optional<MatchEvent> findByMatchIdAndProviderEventId(UUID matchId, String providerEventId);

    boolean existsByMatchIdAndProviderEventId(UUID matchId, String providerEventId);

    @Query("SELECT e FROM MatchEvent e WHERE e.match.id = :matchId ORDER BY e.occurredAt DESC")
    List<MatchEvent> findRecentEventsByMatchId(@Param("matchId") UUID matchId);
}