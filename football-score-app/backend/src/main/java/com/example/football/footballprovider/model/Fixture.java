package com.example.football.footballprovider.model;

import java.time.OffsetDateTime;

public record Fixture(
        String providerId,
        String competitionId,
        String seasonId,
        String homeTeamProviderId,
        String awayTeamProviderId,
        OffsetDateTime scheduledStart,
        String status,
        Integer homeScore,
        Integer awayScore,
        Integer minute,
        Integer extraMinute,
        OffsetDateTime lastUpdated
) {}
