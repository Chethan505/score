package com.example.football.notification;

import com.example.football.auth.User;
import com.example.football.event.MatchEvent;
import com.example.football.favorite.FavoriteMatch;
import com.example.football.favorite.FavoriteMatchRepository;
import com.example.football.favorite.FavoriteTeam;
import com.example.football.favorite.FavoriteTeamRepository;
import com.example.football.footballprovider.model.MatchEvent as ProviderMatchEvent;
import com.example.football.match.Match;
import com.example.football.match.MatchRepository;
import com.example.football.notification.NotificationPreferenceRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class NotificationServiceTest {

    @Mock
    private NotificationRepository notificationRepository;
    @Mock
    private NotificationAttemptRepository attemptRepository;
    @Mock
    private FavoriteMatchRepository favoriteMatchRepository;
    @Mock
    private FavoriteTeamRepository favoriteTeamRepository;
    @Mock
    private NotificationQueueService queueService;
    @Mock
    private MatchRepository matchRepository;
    @Mock
    private MatchEventRepository matchEventRepository;

    private NotificationService notificationService;

    @BeforeEach
    void setUp() {
        notificationService = new NotificationService(
                notificationRepository,
                attemptRepository,
                favoriteMatchRepository,
                favoriteTeamRepository,
                queueService
        );
    }

    @Test
    void createMatchStartReminder_createsNotification_forFavoriteMatchUsers() {
        // Given
        Match match = createMatch();
        User user = createUser();
        FavoriteMatch favorite = new FavoriteMatch();
        favorite.setUser(user);
        favorite.setMatch(match);

        when(favoriteMatchRepository.findByMatchId(match.getId())).thenReturn(List.of(favorite));
        when(favoriteTeamRepository.findByTeamId(match.getHomeTeam().getId())).thenReturn(List.of());
        when(favoriteTeamRepository.findByTeamId(match.getAwayTeam().getId())).thenReturn(List.of());
        when(notificationRepository.findByMatchIdAndType(match.getId(), "MATCH_START")).thenReturn(List.of());
        when(notificationRepository.save(any(Notification.class))).thenAnswer(inv -> inv.getArgument(0));

        // When
        notificationService.createMatchStartReminder(match, 15);

        // Then
        verify(notificationRepository).save(argThat(n -> 
                n.getUser().getId().equals(user.getId()) &&
                n.getType().equals("MATCH_START") &&
                n.getTemplateName().equals("match_starting_soon") &&
                n.getStatus().equals("PENDING")
        ));
        verify(queueService).enqueue(any(UUID.class));
    }

    @Test
    void createGoalNotification_createsNotification_forFavoriteMatchUsers() {
        // Given
        Match match = createMatch();
        User user = createUser();
        FavoriteMatch favorite = new FavoriteMatch();
        favorite.setUser(user);
        favorite.setMatch(match);

        ProviderMatchEvent goal = new ProviderMatchEvent(
                "event-1", "GOAL", 32, 0, "team-home", "player-1", "Player", "Goal", "{}"
        );

        when(favoriteMatchRepository.findByMatchId(match.getId())).thenReturn(List.of(favorite));
        when(favoriteTeamRepository.findByTeamId(any())).thenReturn(List.of());
        when(notificationRepository.findByMatchIdAndType(match.getId(), "GOAL")).thenReturn(List.of());
        when(notificationRepository.save(any(Notification.class))).thenAnswer(inv -> inv.getArgument(0));

        // When
        notificationService.createGoalNotification(match, goal, true, 1, 0);

        // Then
        verify(notificationRepository).save(argThat(n -> 
                n.getUser().getId().equals(user.getId()) &&
                n.getType().equals("GOAL") &&
                n.getTemplateName().equals("goal_notification")
        ));
        verify(queueService).enqueue(any(UUID.class));
    }

    @Test
    void createGoalNotification_preventsDuplicate_forSameUser() {
        // Given
        Match match = createMatch();
        User user = createUser();
        FavoriteMatch favorite = new FavoriteMatch();
        favorite.setUser(user);
        favorite.setMatch(match);

        Notification existingNotification = new Notification();
        existingNotification.setUser(user);
        existingNotification.setMatch(match);
        existingNotification.setType("GOAL");
        existingNotification.setStatus("SENT");

        when(favoriteMatchRepository.findByMatchId(match.getId())).thenReturn(List.of(favorite));
        when(favoriteTeamRepository.findByTeamId(any())).thenReturn(List.of());
        when(notificationRepository.findByMatchIdAndType(match.getId(), "GOAL")).thenReturn(List.of(existingNotification));

        // When
        ProviderMatchEvent goal = new ProviderMatchEvent(
                "event-1", "GOAL", 32, 0, "team-home", "player-1", "Player", "Goal", "{}"
        );
        notificationService.createGoalNotification(match, goal, true, 1, 0);

        // Then
        verify(notificationRepository, never()).save(any(Notification.class));
    }

    @Test
    void createEventNotification_usesPreferences_forCardNotifications() {
        // Given
        Match match = createMatch();
        User user = createUser();
        FavoriteMatch favorite = new FavoriteMatch();
        favorite.setUser(user);
        favorite.setMatch(match);

        // Create preferences with card notifications disabled
        com.example.football.notification.NotificationPreference prefs = 
                com.example.football.notification.NotificationPreference.builder()
                        .user(user)
                        .cardNotifications(false)
                        .build();

        when(favoriteMatchRepository.findByMatchId(match.getId())).thenReturn(List.of(favorite));
        when(favoriteTeamRepository.findByTeamId(any())).thenReturn(List.of());
        when(notificationRepository.findByMatchIdAndType(match.getId(), "YELLOW_CARD")).thenReturn(List.of());
        when(notificationRepository.save(any(Notification.class))).thenAnswer(inv -> inv.getArgument(0));

        // When
        ProviderMatchEvent event = new ProviderMatchEvent(
                "event-1", "YELLOW_CARD", 45, 0, "team-home", "player-1", "Player", "Yellow Card", "{}"
        );
        notificationService.createEventNotification(match, event, "match_event");

        // Then
        verify(notificationRepository, never()).save(any(Notification.class));
    }

    private Match createMatch() {
        Match match = new Match();
        match.setId(UUID.randomUUID());
        match.setProviderMatchId("match-1");
        match.setHomeTeam(createTeam("team-home", "Arsenal"));
        match.setAwayTeam(createTeam("team-away", "Chelsea"));
        match.setCompetition(createCompetition());
        return match;
    }

    private User createUser() {
        User user = new User();
        user.setId(UUID.randomUUID());
        user.setPhoneNumber("+919876543210");
        user.setWhatsappOptIn(true);
        return user;
    }

    private com.example.football.team.Team createTeam(String providerId, String name) {
        com.example.football.team.Team team = new com.example.football.team.Team();
        team.setId(UUID.randomUUID());
        team.setProviderTeamId(providerId);
        team.setName(name);
        return team;
    }

    private com.example.football.competition.Competition createCompetition() {
        com.example.football.competition.Competition comp = new com.example.football.competition.Competition();
        comp.setId(UUID.randomUUID());
        comp.setName("Premier League");
        return comp;
    }
}