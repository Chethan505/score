package com.example.football.competition;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/competitions")
public class CompetitionController {

    private final CompetitionService competitionService;

    public CompetitionController(CompetitionService competitionService) {
        this.competitionService = competitionService;
    }

    @GetMapping
    public ResponseEntity<List<CompetitionDto>> getAllCompetitions() {
        return ResponseEntity.ok(competitionService.getAllCompetitions());
    }

    @GetMapping("/{id}")
    public ResponseEntity<CompetitionDto> getCompetition(@PathVariable UUID id) {
        return competitionService.getCompetition(id)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @GetMapping("/{id}/matches")
    public ResponseEntity<List<MatchSummaryDto>> getCompetitionMatches(
            @PathVariable UUID id,
            @RequestParam(required = false) String status,
            @RequestParam(required = false) String date
    ) {
        return ResponseEntity.ok(competitionService.getMatches(id, status, date));
    }
}