package com.example.football.footballprovider.sportmonks;

import com.example.football.footballprovider.FootballDataProvider;
import com.example.football.footballprovider.model.*;
import com.example.football.footballprovider.sportmonks.dto.*;
import org.springframework.stereotype.Component;

import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.*;
import java.util.stream.Collectors;

@Component
public class SportmonksProvider implements FootballDataProvider {

    private final SportmonksClient client;
    private final Set<String> targetCompetitionIds = Set.of(
            "8",   // Premier League
            "564", // La Liga
            "384", // Serie A
            "2",   // UEFA Champions League
            "305"  // Indian Super League
    );

    public SportmonksProvider(SportmonksClient client) {
        this.client = client;
    }

    @Override
    public List<Competition> getCompetitions() {
        return client.getLeagues().stream()
                .filter(league -> targetCompetitionIds.contains(String.valueOf(league.id())))
                .map(this::mapCompetition)
                .toList();
    }

    @Override
    public List<Competition> getCompetitionsByIds(List<String> providerCompetitionIds) {
        return client.getLeagues().stream()
                .filter(league -> providerCompetitionIds.contains(String.valueOf(league.id())))
                .map(this::mapCompetition)
                .toList();
    }

    @Override
    public List<Fixture> getFixtures(String competitionId, OffsetDateTime from, OffsetDateTime to) {
        int leagueId = Integer.parseInt(competitionId);
        int seasonId = getCurrentSeasonId(leagueId);
        
        String fromStr = from.toInstant().atOffset(ZoneOffset.UTC).toLocalDate().toString();
        String toStr = to.toInstant().atOffset(ZoneOffset.UTC).toLocalDate().toString();
        
        return client.getFixtures(leagueId, seasonId, fromStr, toStr).stream()
                .map(f -> mapFixture(f, competitionId, String.valueOf(seasonId)))
                .toList();
    }

    @Override
    public List<Fixture> getLiveFixtures() {
        return client.getLiveFixtures().stream()
                .filter(f -> targetCompetitionIds.contains(String.valueOf(f.leagueId())))
                .map(f -> mapFixture(f, String.valueOf(f.leagueId()), String.valueOf(f.seasonId())))
                .toList();
    }

    @Override
    public List<Fixture> getUpcomingFixtures(OffsetDateTime from, OffsetDateTime to) {
        return targetCompetitionIds.stream()
                .flatMap(compId -> getFixtures(compId, from, to).stream())
                .filter(f -> f.scheduledStart().isAfter(from) && f.scheduledStart().isBefore(to))
                .toList();
    }

    @Override
    public MatchDetails getMatchDetails(String providerMatchId) {
        long fixtureId = Long.parseLong(providerMatchId);
        SportmonksFixture fixture = client.getFixtureById(fixtureId);
        if (fixture == null) {
            return null;
        }
        return mapMatchDetails(fixture);
    }

    @Override
    public List<Standing> getStandings(String competitionId, String seasonId) {
        int leagueId = Integer.parseInt(competitionId);
        int seasonIdInt = Integer.parseInt(seasonId);
        return client.getStandings(leagueId, seasonIdInt).stream()
                .map(this::mapStanding)
                .toList();
    }

    @Override
    public List<MatchEvent> getMatchEvents(String providerMatchId) {
        long fixtureId = Long.parseLong(providerMatchId);
        SportmonksFixture fixture = client.getFixtureById(fixtureId);
        if (fixture == null || fixture.events() == null) {
            return List.of();
        }
        return fixture.events().stream()
                .map(this::mapEvent)
                .toList();
    }

    @Override
    public void syncCompetitionsAndTeams() {
        // Fetch all target competitions
        List<SportmonksLeague> leagues = client.getLeagues().stream()
                .filter(l -> targetCompetitionIds.contains(String.valueOf(l.id())))
                .toList();

        for (SportmonksLeague league : leagues) {
            int leagueId = league.id();
            int seasonId = getCurrentSeasonId(leagueId);
            
            // Sync teams for this league/season
            List<SportmonksTeam> teams = client.getTeams(leagueId, seasonId);
            // Teams will be persisted by the sync service
        }
    }

    @Override
    public void syncFixturesForCompetition(String competitionId, String seasonId) {
        // Implemented in MatchSyncService
    }

    private int getCurrentSeasonId(int leagueId) {
        List<SportmonksSeason> seasons = client.getSeasons(leagueId);
        return seasons.stream()
                .filter(SportmonksSeason::isCurrent)
                .findFirst()
                .map(SportmonksSeason::id)
                .orElse(seasons.stream()
                        .max(Comparator.comparing(SportmonksSeason::startingDate))
                        .map(SportmonksSeason::id)
                        .orElse(0));
    }

    private Competition mapCompetition(SportmonksLeague league) {
        List<Season> seasons = client.getSeasons(league.id()).stream()
                .map(s -> new Season(
                        String.valueOf(s.id()),
                        s.name(),
                        OffsetDateTime.parse(s.startingDate()),
                        OffsetDateTime.parse(s.endingDate()),
                        s.isCurrent()
                ))
                .toList();

        return new Competition(
                String.valueOf(league.id()),
                league.name(),
                league.countryName(),
                league.imagePath(),
                true,
                seasons
        );
    }

    private Fixture mapFixture(SportmonksFixture fixture, String competitionId, String seasonId) {
        String homeTeamId = fixture.participants().stream()
                .filter(p -> "home".equals(p.meta().location()))
                .map(p -> String.valueOf(p.id()))
                .findFirst()
                .orElse("");

        String awayTeamId = fixture.participants().stream()
                .filter(p -> "away".equals(p.meta().location()))
                .map(p -> String.valueOf(p.id()))
                .findFirst()
                .orElse("");

        Integer homeScore = fixture.scores().stream()
                .filter(s -> "CURRENT".equals(s.description()) || "FT".equals(s.description()))
                .map(s -> s.score().goals())
                .findFirst()
                .orElse(0);

        Integer awayScore = fixture.scores().stream()
                .filter(s -> "CURRENT".equals(s.description()) || "FT".equals(s.description()))
                .map(s -> s.score().goals())
                .skip(1)
                .findFirst()
                .orElse(0);

        String status = mapStateId(fixture.stateId());

        return new Fixture(
                String.valueOf(fixture.id()),
                competitionId,
                seasonId,
                homeTeamId,
                awayTeamId,
                fixture.startingAt(),
                status,
                homeScore,
                awayScore,
                fixture.time().minute(),
                fixture.time().extraMinute(),
                OffsetDateTime.now()
        );
    }

    private MatchDetails mapMatchDetails(SportmonksFixture fixture) {
        Team homeTeam = mapTeam(fixture.participants().stream()
                .filter(p -> "home".equals(p.meta().location()))
                .findFirst()
                .orElseThrow());

        Team awayTeam = mapTeam(fixture.participants().stream()
                .filter(p -> "away".equals(p.meta().location()))
                .findFirst()
                .orElseThrow());

        List<MatchEvent> events = fixture.events() != null ? fixture.events().stream()
                .map(this::mapEvent)
                .toList() : List.of();

        List<Lineup> homeLineup = fixture.lineups() != null && fixture.lineups().home() != null ?
                fixture.lineups().home().stream().map(this::mapLineup).toList() : List.of();

        List<Lineup> awayLineup = fixture.lineups() != null && fixture.lineups().away() != null ?
                fixture.lineups().away().stream().map(this::mapLineup).toList() : List.of();

        Statistics statistics = fixture.statistics() != null ? mapStatistics(fixture.statistics()) : null;

        Integer homeScore = fixture.scores().stream()
                .filter(s -> "CURRENT".equals(s.description()) || "FT".equals(s.description()))
                .map(s -> s.score().goals())
                .findFirst()
                .orElse(0);

        Integer awayScore = fixture.scores().stream()
                .filter(s -> "CURRENT".equals(s.description()) || "FT".equals(s.description()))
                .map(s -> s.score().goals())
                .skip(1)
                .findFirst()
                .orElse(0);

        return new MatchDetails(
                String.valueOf(fixture.id()),
                String.valueOf(fixture.leagueId()),
                String.valueOf(fixture.seasonId()),
                homeTeam,
                awayTeam,
                fixture.startingAt(),
                mapStateId(fixture.stateId()),
                homeScore,
                awayScore,
                fixture.time().minute(),
                fixture.time().extraMinute(),
                events,
                homeLineup,
                awayLineup,
                statistics,
                OffsetDateTime.now()
        );
    }

    private Team mapTeam(SportmonksParticipant participant) {
        return new Team(
                String.valueOf(participant.id()),
                participant.name(),
                "", // short name not in participant
                participant.imagePath(),
                ""
        );
    }

    private Lineup mapLineup(SportmonksLineupPlayer player) {
        return new Lineup(
                String.valueOf(player.playerId()),
                player.playerName(),
                player.jerseyNumber(),
                player.position(),
                player.isStarter(),
                player.substitutedIn(),
                player.substitutedOut()
        );
    }

    private Statistics mapStatistics(SportmonksStatistics stats) {
        Map<String, Object> homeStats = stats.home().stream()
                .collect(Collectors.toMap(
                        SportmonksStatistic::type,
                        s -> s.value()
                ));

        Map<String, Object> awayStats = stats.away().stream()
                .collect(Collectors.toMap(
                        SportmonksStatistic::type,
                        s -> s.value()
                ));

        return new Statistics(
                getInt(homeStats, "Ball Possession"),
                getInt(awayStats, "Ball Possession"),
                getInt(homeStats, "Shots"),
                getInt(awayStats, "Shots"),
                getInt(homeStats, "Shots On Target"),
                getInt(awayStats, "Shots On Target"),
                getInt(homeStats, "Corner Kicks"),
                getInt(awayStats, "Corner Kicks"),
                getInt(homeStats, "Fouls"),
                getInt(awayStats, "Fouls"),
                getInt(homeStats, "Offsides"),
                getInt(awayStats, "Offsides"),
                getInt(homeStats, "Yellow Cards"),
                getInt(awayStats, "Yellow Cards"),
                getInt(homeStats, "Red Cards"),
                getInt(awayStats, "Red Cards")
        );
    }

    private Integer getInt(Map<String, Object> map, String key) {
        Object value = map.get(key);
        if (value instanceof Number) {
            return ((Number) value).intValue();
        }
        if (value instanceof String) {
            try {
                return Integer.parseInt((String) value);
            } catch (NumberFormatException e) {
                return null;
            }
        }
        return null;
    }

    private MatchEvent mapEvent(SportmonksEvent event) {
        return new MatchEvent(
                String.valueOf(event.id()),
                mapEventType(event.type()),
                event.minute(),
                event.extraMinute(),
                String.valueOf(event.teamId()),
                event.playerId() != null ? String.valueOf(event.playerId()) : null,
                event.playerName(),
                event.description(),
                event.information() != null ? String.join(", ", event.information()) : null
        );
    }

    private Standing mapStanding(SportmonksStanding standing) {
        return new Standing(
                String.valueOf(standing.teamId()),
                standing.position(),
                standing.overall().played(),
                standing.overall().won(),
                standing.overall().draw(),
                standing.overall().lost(),
                standing.overall().goalsFor(),
                standing.overall().goalsAgainst(),
                standing.overall().goalDifference(),
                standing.overall().points(),
                standing.overall().form()
        );
    }

    private String mapStateId(int stateId) {
        return switch (stateId) {
            case 1 -> "SCHEDULED";      // Not Started
            case 2 -> "LIVE";           // Live
            case 3 -> "HALFTIME";       // Half Time
            case 4 -> "FINISHED";       // Finished
            case 5 -> "POSTPONED";      // Postponed
            case 6 -> "CANCELLED";      // Cancelled
            case 7 -> "ABANDONED";      // Abandoned
            case 8 -> "DELAYED";        // Delayed
            case 9 -> "INTERRUPTED";    // Interrupted
            case 10 -> "SUSPENDED";     // Suspended
            case 11 -> "EXTRA_TIME";    // Extra Time
            case 12 -> "PENALTIES";     // Penalty Shootout
            case 13 -> "BREAK";         // Break
            case 14 -> "AWARDED";       // Awarded
            default -> "UNKNOWN";
        };
    }

    private String mapEventType(String sportmonksType) {
        return switch (sportmonksType.toLowerCase()) {
            case "goal", "penalty_goal", "own_goal" -> "GOAL";
            case "yellow_card" -> "YELLOW_CARD";
            case "red_card" -> "RED_CARD";
            case "substitution" -> "SUBSTITUTION";
            case "half_time" -> "HALFTIME";
            case "full_time" -> "FULLTIME";
            case "extra_time_start" -> "EXTRA_TIME_START";
            case "penalty_shootout" -> "PENALTY_SHOOTOUT";
            case "var" -> "VAR";
            case "missed_penalty" -> "MISSED_PENALTY";
            case "goal_disallowed" -> "GOAL_DISALLOWED";
            default -> sportmonksType.toUpperCase();
        };
    }
}