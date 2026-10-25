package com.example.football.match;

public record StatisticsDto(
        Integer possessionHome,
        Integer possessionAway,
        Integer shotsHome,
        Integer shotsAway,
        Integer shotsOnTargetHome,
        Integer shotsOnTargetAway,
        Integer cornersHome,
        Integer cornersAway,
        Integer foulsHome,
        Integer foulsAway,
        Integer offsidesHome,
        Integer offsidesAway,
        Integer yellowCardsHome,
        Integer yellowCardsAway,
        Integer redCardsHome,
        Integer redCardsAway
) {}