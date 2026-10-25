package com.example.football.match;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/matches")
public class MatchController {

    private final MatchService matchService;

    public MatchController(MatchService matchService) {
        this.matchService = matchService;
    }

    @GetMapping("/live")
    public ResponseEntity<List<MatchSummaryDto>> getLiveMatches() {
        return ResponseEntity.ok(matchService.getLiveMatches());
    }

    @GetMapping("/upcoming")
    public ResponseEntity<List<MatchSummaryDto>> getUpcomingMatches(
            @RequestParam(defaultValue = "24") int hours
    ) {
        return ResponseEntity.ok(matchService.getUpcomingMatches(hours));
    }

    @GetMapping("/finished")
    public ResponseEntity<List<MatchSummaryDto>> getFinishedMatches(
            @RequestParam(defaultValue = "7") int days
    ) {
        return ResponseEntity.ok(matchService.getFinishedMatches(days));
    }

    @GetMapping("/{id}")
    public ResponseEntity<MatchDetailDto> getMatchDetail(@PathVariable UUID id) {
        return matchService.getMatchDetail(id)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @GetMapping("/{id}/events")
    public ResponseEntity<List<MatchEventDto>> getMatchEvents(@PathVariable UUID id) {
        return ResponseEntity.ok(matchService.getMatchEvents(id));
    }

    @PostMapping("/{id}/favorite")
    public ResponseEntity<Void> favoriteMatch(@CurrentUser UUID userId, @PathVariable UUID id) {
        matchService.favoriteMatch(userId, id);
        return ResponseEntity.ok().build();
    }

    @DeleteMapping("/{id}/favorite")
    public ResponseEntity<Void> unfavoriteMatch(@CurrentUser UUID userId, @PathVariable UUID id) {
        matchService.unfavoriteMatch(userId, id);
        return ResponseEntity.ok().build();
    }
}