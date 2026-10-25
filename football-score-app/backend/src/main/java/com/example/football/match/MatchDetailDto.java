package com.example.football.match;

import com.example.football.event.MatchEventDto;
import com.example.football.team.TeamDto;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

public record MatchDetailDto(
        UUID id,
        String providerMatchId,
        UUID competitionId,
        String competitionName,
        TeamDto homeTeam,
        TeamDto awayTeam,
        OffsetDateTime scheduledStart,
        String status,
        Integer homeScore,
        Integer awayScore,
        Integer minute,
        Integer extraMinute,
        List<MatchEventDto> events,
        List<LineupDto> homeLineup,
        List<LineupDto> awayLineup,
        StatisticsDto statistics
) {}