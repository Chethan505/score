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
import net.javacrumbs.shedlock.spring.annotation.SchedulerLock;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;
import java.util.*;
import java.util.stream.Collectors;

@Service
@Transactional
public class MatchSyncService {

    private static final Logger log = LoggerFactory.getLogger(MatchSyncService.class);

    private final FootballDataProvider footballProvider;
    private final MatchRepository matchRepository;
    private final CompetitionRepository competitionRepository;
    private final TeamRepository teamRepository;
    private final SeasonRepository seasonRepository;
    private final MatchEventRepository matchEventRepository;
    private final NotificationRepository notificationRepository;
    private final NotificationService notificationService;
    private final StandingRepository standingRepository;

    @Value("${scheduler.live-match-poll-interval-seconds:30}")
    private int livePollIntervalSeconds;

    @Value("${scheduler.upcoming-match-poll-interval-minutes:5}")
    private int upcomingPollIntervalMinutes;

    @Value("${scheduler.match-start-reminder-minutes:15}")
    private int matchStartReminderMinutes;

    public MatchSyncService(
            FootballDataProvider footballProvider,
            MatchRepository matchRepository,
            CompetitionRepository competitionRepository,
            TeamRepository teamRepository,
            SeasonRepository seasonRepository,
            MatchEventRepository matchEventRepository,
            NotificationRepository notificationRepository,
            NotificationService notificationService,
            StandingRepository standingRepository
    ) {
        this.footballProvider = footballProvider;
        this.matchRepository = matchRepository;
        this.competitionRepository = competitionRepository;
        this.teamRepository = teamRepository;
        this.seasonRepository = seasonRepository;
        this.matchEventRepository = matchEventRepository;
        this.notificationRepository = notificationRepository;
        this.notificationService = notificationService;
        this.standingRepository = standingRepository;
    }

    @Scheduled(fixedDelayString = "${scheduler.live-match-poll-interval-seconds:30}000")
    @SchedulerLock(name = "syncLiveMatches", lockAtMostFor = "25s", lockAtLeastFor = "5s")
    public void syncLiveMatches() {
        log.debug("Starting live match sync");
        try {
            List<Fixture> liveFixtures = footballProvider.getLiveFixtures();
            log.info("Fetched {} live fixtures from provider", liveFixtures.size());

            for (Fixture fixture : liveFixtures) {
                syncSingleMatch(fixture);
            }
        } catch (Exception e) {
            log.error("Error syncing live matches", e);
        }
    }

    @Scheduled(fixedDelayString = "${scheduler.upcoming-match-poll-interval-minutes:5}00000")
    @SchedulerLock(name = "syncUpcomingMatches", lockAtMostFor = "4m", lockAtLeastFor = "1m")
    public void syncUpcomingMatches() {
        log.debug("Starting upcoming match sync");
        try {
            OffsetDateTime now = OffsetDateTime.now();
            OffsetDateTime soon = now.plusMinutes(upcomingPollIntervalMinutes * 2);
            
            List<Fixture> upcomingFixtures = footballProvider.getUpcomingFixtures(now, soon);
            log.info("Fetched {} upcoming fixtures from provider", upcomingFixtures.size());

            for (Fixture fixture : upcomingFixtures) {
                syncSingleMatch(fixture);
            }

            checkMatchStartReminders(now);
        } catch (Exception e) {
            log.error("Error syncing upcoming matches", e);
        }
    }

    @Scheduled(fixedDelayString = "${scheduler.full-sync-interval-hours:6}000000")
    @SchedulerLock(name = "fullSync", lockAtMostFor = "5m", lockAtLeastFor = "1m")
    public void fullSync() {
        log.info("Starting full sync of all competitions");
        try {
            List<com.example.football.footballprovider.model.Competition> competitions = footballProvider.getCompetitions();
            
            for (com.example.football.footballprovider.model.Competition comp : competitions) {
                syncCompetition(comp);
            }
            
            log.info("Full sync completed");
        } catch (Exception e) {
            log.error("Error during full sync", e);
        }
    }

    private void syncCompetition(com.example.football.footballprovider.model.Competition providerComp) {
        Competition competition = competitionRepository.findByProviderCompetitionId(providerComp.providerId())
                .orElseGet(() -> createCompetition(providerComp));

        for (com.example.football.footballprovider.model.Season providerSeason : providerComp.seasons()) {
            Season season = seasonRepository.findByProviderSeasonId(providerSeason.providerId())
                    .orElseGet(() -> createSeason(competition, providerSeason));

            if (providerSeason.isCurrent()) {
                syncFixturesForSeason(competition, season);
            }
        }
    }

    private void syncFixturesForSeason(Competition competition, Season season) {
        OffsetDateTime from = OffsetDateTime.now().minusDays(7);
        OffsetDateTime to = OffsetDateTime.now().plusDays(30);
        
        List<Fixture> fixtures = footballProvider.getFixtures(
                competition.getProviderCompetitionId(), from, to);

        for (Fixture fixture : fixtures) {
            syncSingleMatch(fixture);
        }
    }

    private void syncSingleMatch(Fixture providerFixture) {
        Match match = matchRepository.findByProviderMatchId(providerFixture.providerId())
                .orElseGet(() -> createMatch(providerFixture));

        String oldStatus = match.getStatus();
        Integer oldHomeScore = match.getHomeScore();
        Integer oldAwayScore = match.getAwayScore();
        Integer oldMinute = match.getMatchMinute();

        updateMatchFromFixture(match, providerFixture);
        match.setLastProviderUpdate(OffsetDateTime.now());
        matchRepository.save(match);

        detectAndHandleEvents(match, providerFixture, oldStatus, oldHomeScore, oldAwayScore, oldMinute);
    }

    private Match createMatch(Fixture providerFixture) {
        Competition competition = competitionRepository.findByProviderCompetitionId(providerFixture.competitionId())
                .orElseThrow(() -> new IllegalStateException("Competition not found: " + providerFixture.competitionId()));

        Season season = seasonRepository.findByProviderSeasonId(providerFixture.seasonId())
                .orElse(null);

        Team homeTeam = teamRepository.findByProviderTeamId(providerFixture.homeTeamProviderId())
                .orElseThrow(() -> new IllegalStateException("Home team not found: " + providerFixture.homeTeamProviderId()));

        Team awayTeam = teamRepository.findByProviderTeamId(providerFixture.awayTeamProviderId())
                .orElseThrow(() -> new IllegalStateException("Away team not found: " + providerFixture.awayTeamProviderId()));

        return Match.builder()
                .providerMatchId(providerFixture.providerId())
                .competition(competition)
                .season(season)
                .homeTeam(homeTeam)
                .awayTeam(awayTeam)
                .scheduledStart(providerFixture.scheduledStart())
                .status(providerFixture.status())
                .homeScore(providerFixture.homeScore())
                .awayScore(providerFixture.awayScore())
                .matchMinute(providerFixture.minute())
                .extraMinute(providerFixture.extraMinute())
                .build();
    }

    private Competition createCompetition(com.example.football.footballprovider.model.Competition providerComp) {
        return competitionRepository.save(Competition.builder()
                .providerCompetitionId(providerComp.providerId())
                .name(providerComp.name())
                .country(providerComp.country())
                .logoUrl(providerComp.logoUrl())
                .isActive(true)
                .build());
    }

    private Season createSeason(Competition competition, com.example.football.footballprovider.model.Season providerSeason) {
        return seasonRepository.save(Season.builder()
                .providerSeasonId(providerSeason.providerId())
                .competition(competition)
                .name(providerSeason.name())
                .startDate(providerSeason.startDate().toLocalDate())
                .endDate(providerSeason.endDate().toLocalDate())
                .isCurrent(providerSeason.isCurrent())
                .build());
    }

    private void updateMatchFromFixture(Match match, Fixture fixture) {
        match.setStatus(fixture.status());
        match.setHomeScore(fixture.homeScore());
        match.setAwayScore(fixture.awayScore());
        match.setMatchMinute(fixture.minute());
        match.setExtraMinute(fixture.extraMinute());
    }

    private void detectAndHandleEvents(
            Match match,
            Fixture providerFixture,
            String oldStatus,
            Integer oldHomeScore,
            Integer oldAwayScore,
            Integer oldMinute
    ) {
        String newStatus = match.getStatus();
        Integer newHomeScore = match.getHomeScore();
        Integer newAwayScore = match.getAwayScore();

        if (!newStatus.equals(oldStatus)) {
            handleStatusChange(match, oldStatus, newStatus);
        }

        if (newHomeScore > oldHomeScore) {
            handleGoalEvent(match, providerFixture, true, newHomeScore, newAwayScore);
        }

        if (newAwayScore > oldAwayScore) {
            handleGoalEvent(match, providerFixture, false, newHomeScore, newAwayScore);
        }

        if (providerFixture.minute() != null && !providerFixture.minute().equals(oldMinute)) {
            handleMinuteUpdate(match, oldMinute, providerFixture.minute());
        }

        syncDetailedEvents(match);
    }

    private void handleStatusChange(Match match, String oldStatus, String newStatus) {
        log.info("Match {} status changed: {} -> {}", match.getProviderMatchId(), oldStatus, newStatus);

        if ("LIVE".equals(newStatus) && "SCHEDULED".equals(oldStatus)) {
            notificationService.createMatchStartNotification(match);
        } else if ("HALFTIME".equals(newStatus)) {
            notificationService.createHalfTimeNotification(match);
        } else if ("FINISHED".equals(newStatus)) {
            notificationService.createFullTimeNotification(match);
        } else if ("POSTPONED".equals(newStatus) || "CANCELLED".equals(newStatus)) {
            notificationService.createPostponedCancelledNotification(match, newStatus);
        }
    }

    private void handleGoalEvent(Match match, Fixture fixture, boolean homeTeamScored, int newHomeScore, int newAwayScore) {
        log.info("Goal detected in match {}: {} - {}", match.getProviderMatchId(), newHomeScore, newAwayScore);

        MatchDetails details = footballProvider.getMatchDetails(match.getProviderMatchId());
        if (details != null && details.events() != null) {
            List<ProviderMatchEvent> newGoals = details.events().stream()
                    .filter(e -> "GOAL".equals(e.type()))
                    .filter(e -> homeTeamScored 
                            ? match.getHomeTeam().getProviderTeamId().equals(e.teamProviderId())
                            : match.getAwayTeam().getProviderTeamId().equals(e.teamProviderId()))
                    .toList();

            for (ProviderMatchEvent goal : newGoals) {
                if (!matchEventRepository.existsByMatchIdAndProviderEventId(match.getId(), goal.providerId())) {
                    notificationService.createGoalNotification(match, goal, homeTeamScored, newHomeScore, newAwayScore);
                }
            }
        }
    }

    private void handleMinuteUpdate(Match match, Integer oldMinute, Integer newMinute) {
        if (oldMinute == null) return;
        
        if (newMinute >= 45 && oldMinute < 45 && newMinute < 60) {
            notificationService.createHalfTimeNotification(match);
        }
    }

    private void syncDetailedEvents(Match match) {
        MatchDetails details = footballProvider.getMatchDetails(match.getProviderMatchId());
        if (details == null || details.events() == null) {
            return;
        }

        for (ProviderMatchEvent providerEvent : details.events()) {
            if (!matchEventRepository.existsByMatchIdAndProviderEventId(match.getId(), providerEvent.providerId())) {
                saveMatchEvent(match, providerEvent);
                handleSpecificEvent(match, providerEvent);
            }
        }
    }

    private MatchEvent saveMatchEvent(Match match, ProviderMatchEvent providerEvent) {
        Team team = null;
        if (providerEvent.teamProviderId() != null) {
            team = teamRepository.findByProviderTeamId(providerEvent.teamProviderId()).orElse(null);
        }

        MatchEvent event = MatchEvent.builder()
                .match(match)
                .providerEventId(providerEvent.providerId())
                .eventType(providerEvent.type())
                .team(team)
                .playerId(providerEvent.playerProviderId() != null ? UUID.fromString(providerEvent.playerProviderId()) : null)
                .playerName(providerEvent.playerName())
                .minute(providerEvent.minute())
                .extraMinute(providerEvent.extraMinute())
                .description(providerEvent.description())
                .metadata(providerEvent.rawData())
                .occurredAt(OffsetDateTime.now())
                .build();

        return matchEventRepository.save(event);
    }

    private void handleSpecificEvent(Match match, ProviderMatchEvent event) {
        String eventType = event.type();
        boolean shouldNotify = false;
        String template = null;

        switch (eventType) {
            case "YELLOW_CARD", "RED_CARD" -> {
                shouldNotify = true;
                template = "match_event";
            }
            case "SUBSTITUTION" -> {
                shouldNotify = true;
                template = "match_event";
            }
            case "HALFTIME" -> {
                shouldNotify = true;
                template = "match_event";
            }
            case "FULLTIME" -> {
                shouldNotify = true;
                template = "full_time_result";
            }
        }

        if (shouldNotify) {
            notificationService.createEventNotification(match, event, template);
        }
    }

    private void checkMatchStartReminders(OffsetDateTime now) {
        OffsetDateTime reminderWindow = now.plusMinutes(matchStartReminderMinutes);
        
        List<Match> upcomingMatches = matchRepository.findByStatusAndScheduledStartBetween(
                "SCHEDULED", now, reminderWindow);

        for (Match match : upcomingMatches) {
            boolean reminderExists = notificationRepository.findByMatchIdAndType(match.getId(), "MATCH_START")
                    .stream()
                    .anyMatch(n -> "PENDING".equals(n.getStatus()) || "SENT".equals(n.getStatus()) || "DELIVERED".equals(n.getStatus()));

            if (!reminderExists) {
                notificationService.createMatchStartReminder(match, matchStartReminderMinutes);
            }
        }
    }
}