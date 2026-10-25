package com.example.football.match;

import com.example.football.event.MatchEvent;
import com.example.football.event.MatchEventRepository;
import com.example.football.footballprovider.Fixture;
import com.example.football.footballprovider.FootballDataProvider;
import com.example.football.footballprovider.model.MatchDetails;
import com.example.football.footballprovider.model.MatchEvent as ProviderMatchEvent;
import com.example.football.notification.Notification;
import com.example.football.notification.NotificationRepository;
import com.example.football.notification.NotificationService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class MatchSyncServiceTest {

    @Mock
    private FootballDataProvider footballProvider;
    @Mock
    private MatchRepository matchRepository;
    @Mock
    private CompetitionRepository competitionRepository;
    @Mock
    private TeamRepository teamRepository;
    @Mock
    private SeasonRepository seasonRepository;
    @Mock
    private MatchEventRepository matchEventRepository;
    @Mock
    private NotificationRepository notificationRepository;
    @Mock
    private NotificationService notificationService;
    @Mock
    private StandingRepository standingRepository;

    private MatchSyncService matchSyncService;

    @BeforeEach
    void setUp() {
        matchSyncService = new MatchSyncService(
                footballProvider,
                matchRepository,
                competitionRepository,
                teamRepository,
                seasonRepository,
                matchEventRepository,
                notificationRepository,
                notificationService,
                standingRepository
        );
    }

    @Test
    void syncSingleMatch_createsNewMatch_whenNotExists() {
        // Given
        Fixture fixture = createFixture("match-1", "SCHEDULED", 0, 0);
        when(matchRepository.findByProviderMatchId("match-1")).thenReturn(Optional.empty());
        when(competitionRepository.findByProviderCompetitionId("comp-1")).thenReturn(Optional.of(createCompetition()));
        when(teamRepository.findByProviderTeamId("team-home")).thenReturn(Optional.of(createHomeTeam()));
        when(teamRepository.findByProviderTeamId("team-away")).thenReturn(Optional.of(createAwayTeam()));
        when(matchRepository.save(any(Match.class))).thenAnswer(inv -> inv.getArgument(0));

        // When
        matchSyncService.syncSingleMatch(fixture);

        // Then
        verify(matchRepository).save(argThat(match -> 
                match.getProviderMatchId().equals("match-1") &&
                match.getStatus().equals("SCHEDULED") &&
                match.getHomeScore() == 0 &&
                match.getAwayScore() == 0
        ));
    }

    @Test
    void syncSingleMatch_updatesExistingMatch_whenStatusChanges() {
        // Given
        Fixture fixture = createFixture("match-1", "LIVE", 1, 0);
        Match existingMatch = createExistingMatch("match-1", "SCHEDULED", 0, 0);
        when(matchRepository.findByProviderMatchId("match-1")).thenReturn(Optional.of(existingMatch));
        when(matchRepository.save(any(Match.class))).thenAnswer(inv -> inv.getArgument(0));
        when(footballProvider.getMatchDetails("match-1")).thenReturn(null);

        // When
        matchSyncService.syncSingleMatch(fixture);

        // Then
        verify(matchRepository).save(argThat(match -> 
                match.getStatus().equals("LIVE") &&
                match.getHomeScore() == 1 &&
                match.getAwayScore() == 0
        ));
        verify(notificationService).createMatchStartNotification(any(Match.class));
    }

    @Test
    void syncSingleMatch_detectsGoal_whenHomeScoreIncreases() {
        // Given
        Fixture fixture = createFixture("match-1", "LIVE", 1, 0);
        Match existingMatch = createExistingMatch("match-1", "LIVE", 0, 0);
        when(matchRepository.findByProviderMatchId("match-1")).thenReturn(Optional.of(existingMatch));
        when(matchRepository.save(any(Match.class))).thenAnswer(inv -> inv.getArgument(0));
        
        MatchDetails details = createMatchDetailsWithGoal("match-1", true);
        when(footballProvider.getMatchDetails("match-1")).thenReturn(details);
        when(matchEventRepository.existsByMatchIdAndProviderEventId(any(), any())).thenReturn(false);

        // When
        matchSyncService.syncSingleMatch(fixture);

        // Then
        verify(notificationService).createGoalNotification(any(Match.class), any(ProviderMatchEvent.class), eq(true), eq(1), eq(0));
    }

    @Test
    void syncSingleMatch_preventsDuplicateGoalNotification() {
        // Given
        Fixture fixture = createFixture("match-1", "LIVE", 1, 0);
        Match existingMatch = createExistingMatch("match-1", "LIVE", 0, 0);
        when(matchRepository.findByProviderMatchId("match-1")).thenReturn(Optional.of(existingMatch));
        when(matchRepository.save(any(Match.class))).thenAnswer(inv -> inv.getArgument(0));
        
        MatchDetails details = createMatchDetailsWithGoal("match-1", true);
        when(footballProvider.getMatchDetails("match-1")).thenReturn(details);
        when(matchEventRepository.existsByMatchIdAndProviderEventId(any(), any())).thenReturn(true);

        // When
        matchSyncService.syncSingleMatch(fixture);

        // Then
        verify(notificationService, never()).createGoalNotification(any(), any(), anyBoolean(), anyInt(), anyInt());
    }

    @Test
    void checkMatchStartReminders_createsReminder_forUpcomingMatch() {
        // Given
        OffsetDateTime now = OffsetDateTime.now();
        Match upcomingMatch = createExistingMatch("match-1", "SCHEDULED", 0, 0);
        upcomingMatch.setScheduledStart(now.plusMinutes(10));
        when(matchRepository.findByStatusAndScheduledStartBetween(eq("SCHEDULED"), any(), any()))
                .thenReturn(List.of(upcomingMatch));
        when(notificationRepository.findByMatchIdAndType(any(), eq("MATCH_START")))
                .thenReturn(List.of());

        // When
        matchSyncService.checkMatchStartReminders(now);

        // Then
        verify(notificationService).createMatchStartReminder(eq(upcomingMatch), eq(15));
    }

    private Fixture createFixture(String providerId, String status, int homeScore, int awayScore) {
        return new Fixture(
                providerId,
                "comp-1",
                "season-1",
                "team-home",
                "team-away",
                OffsetDateTime.now().plusHours(1),
                status,
                homeScore,
                awayScore,
                null,
                null,
                OffsetDateTime.now()
        );
    }

    private Competition createCompetition() {
        Competition comp = new Competition();
        comp.setId(UUID.randomUUID());
        comp.setProviderCompetitionId("comp-1");
        comp.setName("Premier League");
        return comp;
    }

    private Team createHomeTeam() {
        Team team = new Team();
        team.setId(UUID.randomUUID());
        team.setProviderTeamId("team-home");
        team.setName("Arsenal");
        return team;
    }

    private Team createAwayTeam() {
        Team team = new Team();
        team.setId(UUID.randomUUID());
        team.setProviderTeamId("team-away");
        team.setName("Chelsea");
        return team;
    }

    private Match createExistingMatch(String providerId, String status, int homeScore, int awayScore) {
        Match match = new Match();
        match.setId(UUID.randomUUID());
        match.setProviderMatchId(providerId);
        match.setStatus(status);
        match.setHomeScore(homeScore);
        match.setAwayScore(awayScore);
        match.setMatchMinute(30);
        match.setCompetition(createCompetition());
        match.setHomeTeam(createHomeTeam());
        match.setAwayTeam(createAwayTeam());
        match.setScheduledStart(OffsetDateTime.now().plusHours(1));
        return match;
    }

    private MatchDetails createMatchDetailsWithGoal(String matchId, boolean homeTeamScored) {
        ProviderMatchEvent goal = new ProviderMatchEvent(
                "event-1",
                "GOAL",
                32,
                0,
                homeTeamScored ? "team-home" : "team-away",
                "player-1",
                "Player Name",
                "Goal",
                "{}"
        );
        return new MatchDetails(
                matchId, "comp-1", "season-1",
                null, null, OffsetDateTime.now(), "LIVE", 1, 0, 32, 0,
                List.of(goal), List.of(), List.of(), null, OffsetDateTime.now()
        );
    }
}