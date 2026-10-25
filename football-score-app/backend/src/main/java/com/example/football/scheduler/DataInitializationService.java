package com.example.football.scheduler;

import com.example.football.competition.Competition;
import com.example.football.competition.CompetitionRepository;
import com.example.football.footballprovider.FootballDataProvider;
import com.example.football.footballprovider.model.Competition as ProviderCompetition;
import com.example.football.match.Season;
import com.example.football.match.SeasonRepository;
import com.example.football.team.Team;
import com.example.football.team.TeamRepository;
import net.javacrumbs.shedlock.spring.annotation.SchedulerLock;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class DataInitializationService implements ApplicationRunner {

    private static final Logger log = LoggerFactory.getLogger(DataInitializationService.class);

    private final FootballDataProvider footballProvider;
    private final CompetitionRepository competitionRepository;
    private final SeasonRepository seasonRepository;
    private final TeamRepository teamRepository;

    public DataInitializationService(
            FootballDataProvider footballProvider,
            CompetitionRepository competitionRepository,
            SeasonRepository seasonRepository,
            TeamRepository teamRepository
    ) {
        this.footballProvider = footballProvider;
        this.competitionRepository = competitionRepository;
        this.seasonRepository = seasonRepository;
        this.teamRepository = teamRepository;
    }

    @Override
    public void run(ApplicationArguments args) {
        if (shouldRunInitialization()) {
            initializeData();
        }
    }

    private boolean shouldRunInitialization() {
        return competitionRepository.count() == 0;
    }

    @Transactional
    @SchedulerLock(name = "dataInitialization", lockAtMostFor = "5m", lockAtLeastFor = "1m")
    public void initializeData() {
        log.info("Starting initial data synchronization");

        try {
            List<ProviderCompetition> providerCompetitions = footballProvider.getCompetitions();
            log.info("Fetched {} competitions from provider", providerCompetitions.size());

            for (ProviderCompetition providerComp : providerCompetitions) {
                syncCompetition(providerComp);
            }

            log.info("Initial data synchronization completed");
        } catch (Exception e) {
            log.error("Error during initial data synchronization", e);
            throw new RuntimeException("Failed to initialize data", e);
        }
    }

    private void syncCompetition(ProviderCompetition providerComp) {
        Competition competition = competitionRepository.findByProviderCompetitionId(providerComp.providerId())
                .orElseGet(() -> {
                    Competition newComp = Competition.builder()
                            .providerCompetitionId(providerComp.providerId())
                            .name(providerComp.name())
                            .country(providerComp.country())
                            .logoUrl(providerComp.logoUrl())
                            .isActive(true)
                            .build();
                    return competitionRepository.save(newComp);
                });

        for (com.example.football.footballprovider.model.Season providerSeason : providerComp.seasons()) {
            Season season = seasonRepository.findByProviderSeasonId(providerSeason.providerId())
                    .orElseGet(() -> {
                        Season newSeason = Season.builder()
                                .providerSeasonId(providerSeason.providerId())
                                .competition(competition)
                                .name(providerSeason.name())
                                .startDate(providerSeason.startDate().toLocalDate())
                                .endDate(providerSeason.endDate().toLocalDate())
                                .isCurrent(providerSeason.isCurrent())
                                .build();
                        return seasonRepository.save(newSeason);
                    });

            if (providerSeason.isCurrent()) {
                syncTeamsForSeason(competition, season);
            }
        }
    }

    private void syncTeamsForSeason(Competition competition, Season season) {
        log.debug("Syncing teams for competition {} season {}", competition.getName(), season.getName());
        // Teams are synced when fixtures are fetched
        // This is a placeholder for explicit team sync if needed
    }
}