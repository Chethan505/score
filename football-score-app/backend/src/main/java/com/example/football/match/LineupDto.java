package com.example.football.match;

import java.util.UUID;

public record LineupDto(
        UUID playerId,
        String playerName,
        Integer number,
        String position,
        Boolean isStarter,
        Integer substitutedInMinute,
        Integer substitutedOutMinute
) {}