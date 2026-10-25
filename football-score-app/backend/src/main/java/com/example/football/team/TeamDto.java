package com.example.football.team;

import java.util.UUID;

public record TeamDto(
        UUID id,
        String providerTeamId,
        String name,
        String shortName,
        String logoUrl
) {}