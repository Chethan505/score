package com.example.football.footballprovider;

import java.time.OffsetDateTime;
import java.util.List;

public interface FootballDataProvider {

    List<Competition> getCompetitions();

    List<Competition> getCompetitionsByIds(List<String> providerCompetitionIds);

    List<Fixture> getFixtures(String competitionId, OffsetDateTime from, OffsetDateTime to);

    List<Fixture> getLiveFixtures();

    List<Fixture> getUpcomingFixtures(OffsetDateTime from, OffsetDateTime to);

    MatchDetails getMatchDetails(String providerMatchId);

    List<Standing> getStandings(String competitionId, String seasonId);

    List<MatchEvent> getMatchEvents(String providerMatchId);

    void syncCompetitionsAndTeams();

    void syncFixturesForCompetition(String competitionId, String seasonId);
}