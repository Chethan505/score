package com.example.football.footballprovider.model;

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
