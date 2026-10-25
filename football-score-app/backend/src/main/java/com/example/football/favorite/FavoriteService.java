package com.example.football.favorite;

import com.example.football.auth.User;
import com.example.football.auth.UserRepository;
import com.example.football.match.Match;
import com.example.football.match.MatchRepository;
import com.example.football.match.MatchSummaryDto;
import com.example.football.team.Team;
import com.example.football.team.TeamDto;
import com.example.football.team.TeamRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@Transactional
public class FavoriteService {

    private final FavoriteMatchRepository favoriteMatchRepository;
    private final FavoriteTeamRepository favoriteTeamRepository;
    private final UserRepository userRepository;
    private final MatchRepository matchRepository;
    private final TeamRepository teamRepository;
    private final MatchService matchService;

    public FavoriteService(
            FavoriteMatchRepository favoriteMatchRepository,
            FavoriteTeamRepository favoriteTeamRepository,
            UserRepository userRepository,
            MatchRepository matchRepository,
            TeamRepository teamRepository,
            MatchService matchService
    ) {
        this.favoriteMatchRepository = favoriteMatchRepository;
        this.favoriteTeamRepository = favoriteTeamRepository;
        this.userRepository = userRepository;
        this.matchRepository = matchRepository;
        this.teamRepository = teamRepository;
        this.matchService = matchService;
    }

    public List<MatchSummaryDto> getFavoriteMatches(UUID userId) {
        List<FavoriteMatch> favorites = favoriteMatchRepository.findByUserId(userId);
        return favorites.stream()
                .map(fav -> matchService.getMatchDetail(fav.getMatch().getId()))
                .filter(Optional::isPresent)
                .map(Optional::get)
                .map(detail -> new MatchSummaryDto(
                        detail.id(),
                        detail.providerMatchId(),
                        detail.homeTeam().id(),
                        detail.homeTeam().name(),
                        detail.homeTeam().shortName(),
                        detail.homeTeam().logoUrl(),
                        detail.awayTeam().id(),
                        detail.awayTeam().name(),
                        detail.awayTeam().shortName(),
                        detail.awayTeam().logoUrl(),
                        detail.scheduledStart(),
                        detail.status(),
                        detail.homeScore(),
                        detail.awayScore(),
                        detail.minute(),
                        detail.extraMinute()
                ))
                .collect(Collectors.toList());
    }

    public List<TeamDto> getFavoriteTeams(UUID userId) {
        List<FavoriteTeam> favorites = favoriteTeamRepository.findByUserId(userId);
        return favorites.stream()
                .map(fav -> mapTeamToDto(fav.getTeam()))
                .collect(Collectors.toList());
    }

    public void favoriteTeam(UUID userId, UUID teamId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("User not found"));

        if (favoriteTeamRepository.existsByUserIdAndTeamId(userId, teamId)) {
            return;
        }

        Team team = teamRepository.findById(teamId)
                .orElseThrow(() -> new IllegalArgumentException("Team not found"));

        FavoriteTeam favorite = FavoriteTeam.builder()
                .user(user)
                .team(team)
                .build();
        favoriteTeamRepository.save(favorite);
    }

    public void unfavoriteTeam(UUID userId, UUID teamId) {
        favoriteTeamRepository.deleteByUserIdAndTeamId(userId, teamId);
    }

    public boolean isTeamFavorited(UUID userId, UUID teamId) {
        return favoriteTeamRepository.existsByUserIdAndTeamId(userId, teamId);
    }

    private TeamDto mapTeamToDto(Team team) {
        return new TeamDto(team.getId(), team.getProviderTeamId(), team.getName(), team.getShortName(), team.getLogoUrl());
    }
}