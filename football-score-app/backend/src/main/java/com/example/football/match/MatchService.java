package com.example.football.match;

import com.example.football.auth.User;
import com.example.football.auth.UserRepository;
import com.example.football.event.MatchEvent;
import com.example.football.event.MatchEventDto;
import com.example.football.event.MatchEventRepository;
import com.example.football.favorite.FavoriteMatch;
import com.example.football.favorite.FavoriteMatchRepository;
import com.example.football.team.Team;
import com.example.football.team.TeamDto;
import com.example.football.team.TeamRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@Transactional
public class MatchService {

    private final MatchRepository matchRepository;
    private final MatchEventRepository matchEventRepository;
    private final FavoriteMatchRepository favoriteMatchRepository;
    private final UserRepository userRepository;
    private final TeamRepository teamRepository;

    public MatchService(
            MatchRepository matchRepository,
            MatchEventRepository matchEventRepository,
            FavoriteMatchRepository favoriteMatchRepository,
            UserRepository userRepository,
            TeamRepository teamRepository
    ) {
        this.matchRepository = matchRepository;
        this.matchEventRepository = matchEventRepository;
        this.favoriteMatchRepository = favoriteMatchRepository;
        this.userRepository = userRepository;
        this.teamRepository = teamRepository;
    }

    public List<MatchSummaryDto> getLiveMatches() {
        List<Match> matches = matchRepository.findCurrentlyLiveMatches();
        return matches.stream()
                .map(this::mapMatchToSummary)
                .collect(Collectors.toList());
    }

    public List<MatchSummaryDto> getUpcomingMatches(int hours) {
        java.time.OffsetDateTime from = java.time.OffsetDateTime.now();
        java.time.OffsetDateTime to = from.plusHours(hours);
        List<Match> matches = matchRepository.findUpcomingMatches(from, to);
        return matches.stream()
                .map(this::mapMatchToSummary)
                .collect(Collectors.toList());
    }

    public List<MatchSummaryDto> getFinishedMatches(int days) {
        java.time.OffsetDateTime from = java.time.OffsetDateTime.now().minusDays(days);
        java.time.OffsetDateTime to = java.time.OffsetDateTime.now();
        List<Match> matches = matchRepository.findByStatusInAndScheduledStartBetween(
                List.of("FINISHED", "POSTPONED", "CANCELLED"), from, to
        );
        return matches.stream()
                .map(this::mapMatchToSummary)
                .collect(Collectors.toList());
    }

    public Optional<MatchDetailDto> getMatchDetail(UUID id) {
        return matchRepository.findById(id).map(this::mapMatchToDetail);
    }

    public List<MatchEventDto> getMatchEvents(UUID matchId) {
        return matchEventRepository.findByMatchIdOrderByOccurredAtAsc(matchId).stream()
                .map(this::mapEventToDto)
                .collect(Collectors.toList());
    }

    public void favoriteMatch(UUID userId, UUID matchId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("User not found"));

        if (favoriteMatchRepository.existsByUserIdAndMatchId(userId, matchId)) {
            return; // Already favorited
        }

        Match match = matchRepository.findById(matchId)
                .orElseThrow(() -> new IllegalArgumentException("Match not found"));

        FavoriteMatch favorite = FavoriteMatch.builder()
                .user(user)
                .match(match)
                .build();
        favoriteMatchRepository.save(favorite);
    }

    public void unfavoriteMatch(UUID userId, UUID matchId) {
        favoriteMatchRepository.deleteByUserIdAndMatchId(userId, matchId);
    }

    public boolean isMatchFavorited(UUID userId, UUID matchId) {
        return favoriteMatchRepository.existsByUserIdAndMatchId(userId, matchId);
    }

    private MatchSummaryDto mapMatchToSummary(Match match) {
        return new MatchSummaryDto(
                match.getId(),
                match.getProviderMatchId(),
                match.getHomeTeam().getId(),
                match.getHomeTeam().getName(),
                match.getHomeTeam().getShortName(),
                match.getHomeTeam().getLogoUrl(),
                match.getAwayTeam().getId(),
                match.getAwayTeam().getName(),
                match.getAwayTeam().getShortName(),
                match.getAwayTeam().getLogoUrl(),
                match.getScheduledStart(),
                match.getStatus(),
                match.getHomeScore(),
                match.getAwayScore(),
                match.getMatchMinute(),
                match.getExtraMinute()
        );
    }

    private MatchDetailDto mapMatchToDetail(Match match) {
        List<MatchEventDto> events = matchEventRepository.findByMatchIdOrderByOccurredAtAsc(match.getId()).stream()
                .map(this::mapEventToDto)
                .collect(Collectors.toList());

        return new MatchDetailDto(
                match.getId(),
                match.getProviderMatchId(),
                match.getCompetition().getId(),
                match.getCompetition().getName(),
                mapTeamToDto(match.getHomeTeam()),
                mapTeamToDto(match.getAwayTeam()),
                match.getScheduledStart(),
                match.getStatus(),
                match.getHomeScore(),
                match.getAwayScore(),
                match.getMatchMinute(),
                match.getExtraMinute(),
                events,
                List.of(), // Lineup not loaded in this query
                null // Statistics not loaded in this query
        );
    }

    private TeamDto mapTeamToDto(Team team) {
        return new TeamDto(team.getId(), team.getProviderTeamId(), team.getName(), team.getShortName(), team.getLogoUrl());
    }

    private MatchEventDto mapEventToDto(MatchEvent event) {
        TeamDto teamDto = event.getTeam() != null ? mapTeamToDto(event.getTeam()) : null;
        return new MatchEventDto(
                event.getId(),
                event.getProviderEventId(),
                event.getEventType(),
                teamDto,
                event.getPlayerId(),
                event.getPlayerName(),
                event.getMinute(),
                event.getExtraMinute(),
                event.getDescription(),
                event.getMetadata(),
                event.getOccurredAt()
        );
    }
}