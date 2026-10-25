package com.example.football.favorite;

import com.example.football.auth.User;
import com.example.football.match.MatchSummaryDto;
import com.example.football.team.TeamDto;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/favorites")
public class FavoriteController {

    private final FavoriteService favoriteService;

    public FavoriteController(FavoriteService favoriteService) {
        this.favoriteService = favoriteService;
    }

    @GetMapping("/matches")
    public ResponseEntity<List<MatchSummaryDto>> getFavoriteMatches(@CurrentUser UUID userId) {
        return ResponseEntity.ok(favoriteService.getFavoriteMatches(userId));
    }

    @GetMapping("/teams")
    public ResponseEntity<List<TeamDto>> getFavoriteTeams(@CurrentUser UUID userId) {
        return ResponseEntity.ok(favoriteService.getFavoriteTeams(userId));
    }

    @PostMapping("/teams/{teamId}")
    public ResponseEntity<Void> favoriteTeam(@CurrentUser UUID userId, @PathVariable UUID teamId) {
        favoriteService.favoriteTeam(userId, teamId);
        return ResponseEntity.ok().build();
    }

    @DeleteMapping("/teams/{teamId}")
    public ResponseEntity<Void> unfavoriteTeam(@CurrentUser UUID userId, @PathVariable UUID teamId) {
        favoriteService.unfavoriteTeam(userId, teamId);
        return ResponseEntity.ok().build();
    }
}