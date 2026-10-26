package com.example.football.footballprovider.model;

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
