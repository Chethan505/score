package com.example.football.match;

import java.time.OffsetDateTime;
import java.util.UUID;

public record MatchSummaryDto(
        UUID id,
        String providerMatchId,
        UUID homeTeamId,
        String homeTeamName,
        String homeTeamShortName,
        String homeTeamLogoUrl,
        UUID awayTeamId,
        String awayTeamName,
        String awayTeamShortName,
        String awayTeamLogoUrl,
        OffsetDateTime scheduledStart,
        String status,
        Integer homeScore,
        Integer awayScore,
        Integer minute,
        Integer extraMinute
) {}