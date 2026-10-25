package com.example.football.competition;

import java.util.UUID;

public record CompetitionDto(
        UUID id,
        String providerCompetitionId,
        String name,
        String country,
        String logoUrl,
        UUID currentSeasonId
) {}