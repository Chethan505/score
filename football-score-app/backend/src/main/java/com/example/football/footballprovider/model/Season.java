package com.example.football.footballprovider.model;

import java.time.OffsetDateTime;

public record Season(
        String providerId,
        String name,
        OffsetDateTime startDate,
        OffsetDateTime endDate,
        boolean isCurrent
) {}
