package com.example.football.footballprovider.model;

import java.time.OffsetDateTime;
import java.util.List;

public record MatchDetails(
        String providerId,
        String competitionId,
        String seasonId,
        Team homeTeam,
        Team awayTeam,
        OffsetDateTime scheduledStart,
        String status,
        Integer homeScore,
        Integer awayScore,
        Integer minute,
        Integer extraMinute,
        List<MatchEvent> events,
        List<Lineup> homeLineup,
        List<Lineup> awayLineup,
        Statistics statistics,
        OffsetDateTime lastUpdated
) {}
