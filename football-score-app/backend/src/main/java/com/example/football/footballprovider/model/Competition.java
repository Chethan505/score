package com.example.football.footballprovider.model;

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
