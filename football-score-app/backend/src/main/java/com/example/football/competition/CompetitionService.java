package com.example.football.competition;

import com.example.football.match.Match;
import com.example.football.match.MatchRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@Transactional(readOnly = true)
public class CompetitionService {

    private final CompetitionRepository competitionRepository;
    private final MatchRepository matchRepository;
    private final StandingRepository standingRepository;
    private final SeasonRepository seasonRepository;

    public CompetitionService(
            CompetitionRepository competitionRepository,
            MatchRepository matchRepository,
            StandingRepository standingRepository,
            SeasonRepository seasonRepository
    ) {
        this.competitionRepository = competitionRepository;
        this.matchRepository = matchRepository;
        this.standingRepository = standingRepository;
        this.seasonRepository = seasonRepository;
    }

    public List<CompetitionDto> getAllCompetitions() {
        return competitionRepository.findByIsActiveTrue().stream()
                .map(this::mapToDto)
                .collect(Collectors.toList());
    }

    public Optional<CompetitionDto> getCompetition(UUID id) {
        return competitionRepository.findById(id).map(this::mapToDto);
    }

    public List<MatchSummaryDto> getMatches(UUID competitionId, String status, String date) {
        OffsetDateTime from = null;
        OffsetDateTime to = null;

        if (date != null) {
            try {
                LocalDate localDate = LocalDate.parse(date);
                from = localDate.atStartOfDay().atOffset(java.time.ZoneOffset.UTC);
                to = localDate.plusDays(1).atStartOfDay().atOffset(java.time.ZoneOffset.UTC);
            } catch (Exception e) {
                // Ignore invalid date
            }
        }

        List<Match> matches;
        if (status != null && !status.isEmpty()) {
            if (from != null && to != null) {
                matches = matchRepository.findByCompetitionIdAndScheduledStartBetween(competitionId, from, to)
                        .stream()
                        .filter(m -> m.getStatus().equalsIgnoreCase(status))
                        .toList();
            } else {
                matches = matchRepository.findByCompetitionIdAndScheduledStartBetween(
                        competitionId,
                        OffsetDateTime.now().minusDays(30),
                        OffsetDateTime.now().plusDays(30)
                ).stream()
                        .filter(m -> m.getStatus().equalsIgnoreCase(status))
                        .toList();
            }
        } else {
            if (from != null && to != null) {
                matches = matchRepository.findByCompetitionIdAndScheduledStartBetween(competitionId, from, to);
            } else {
                matches = matchRepository.findByCompetitionIdAndScheduledStartBetween(
                        competitionId,
                        OffsetDateTime.now().minusDays(7),
                        OffsetDateTime.now().plusDays(30)
                );
            }
        }

        return matches.stream()
                .sorted((a, b) -> {
                    // Live matches first, then by scheduled start
                    if (a.isLive() && !b.isLive()) return -1;
                    if (!a.isLive() && b.isLive()) return 1;
                    return a.getScheduledStart().compareTo(b.getScheduledStart());
                })
                .map(this::mapMatchToSummary)
                .collect(Collectors.toList());
    }

    private CompetitionDto mapToDto(Competition comp) {
        Season currentSeason = seasonRepository.findByCompetitionIdAndIsCurrentTrue(comp.getId()).orElse(null);
        return new CompetitionDto(
                comp.getId(),
                comp.getProviderCompetitionId(),
                comp.getName(),
                comp.getCountry(),
                comp.getLogoUrl(),
                currentSeason != null ? currentSeason.getId() : null
        );
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
}