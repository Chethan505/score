package com.example.football.footballprovider.model;

public record Lineup(
        String playerProviderId,
        String playerName,
        Integer number,
        String position,
        boolean isStarter,
        Integer substitutedInMinute,
        Integer substitutedOutMinute
) {}
