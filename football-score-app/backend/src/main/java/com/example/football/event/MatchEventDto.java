package com.example.football.event;

import com.example.football.team.TeamDto;
import java.time.OffsetDateTime;
import java.util.UUID;

public record MatchEventDto(
        UUID id,
        String providerEventId,
        String eventType,
        TeamDto team,
        UUID playerId,
        String playerName,
        Integer minute,
        Integer extraMinute,
        String description,
        String metadata,
        OffsetDateTime occurredAt
) {}