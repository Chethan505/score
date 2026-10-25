package com.example.football.footballprovider;

import java.time.OffsetDateTime;
import java.util.List;

public record Competition(
        String providerId,
        String name,
        String country,
        String logoUrl,
        boolean isActive,
        List<Season> seasons
) {}

public record Season(
        String providerId,
        String name,
        OffsetDateTime startDate,
        OffsetDateTime endDate,
        boolean isCurrent
) {}

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

public record Team(
        String providerId,
        String name,
        String shortName,
        String logoUrl,
        String competitionId
) {}

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

public record MatchEvent(
        String providerId,
        String type,
        Integer minute,
        Integer extraMinute,
        String teamProviderId,
        String playerProviderId,
        String playerName,
        String description,
        String rawData
) {}

public record Lineup(
        String playerProviderId,
        String playerName,
        Integer number,
        String position,
        boolean isStarter,
        Integer substitutedInMinute,
        Integer substitutedOutMinute
) {}

public record Statistics(
        Integer possessionHome,
        Integer possessionAway,
        Integer shotsHome,
        Integer shotsAway,
        Integer shotsOnTargetHome,
        Integer shotsOnTargetAway,
        Integer cornersHome,
        Integer cornersAway,
        Integer foulsHome,
        Integer foulsAway,
        Integer offsidesHome,
        Integer offsidesAway,
        Integer yellowCardsHome,
        Integer yellowCardsAway,
        Integer redCardsHome,
        Integer redCardsAway
) {}

public record Standing(
        String teamProviderId,
        Integer position,
        Integer played,
        Integer won,
        Integer draw,
        Integer lost,
        Integer goalsFor,
        Integer goalsAgainst,
        Integer goalDifference,
        Integer points,
        String form
) {}