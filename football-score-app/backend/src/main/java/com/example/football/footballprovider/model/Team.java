package com.example.football.footballprovider.model;

public record Team(
        String providerId,
        String name,
        String shortName,
        String logoUrl,
        String competitionId
) {}
