package com.example.football.config;

import com.example.football.footballprovider.FootballDataProvider;
import com.example.football.whatsapp.WhatsAppService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.actuate.health.Health;
import org.springframework.boot.actuate.health.HealthIndicator;
import org.springframework.stereotype.Component;

@Component
public class FootballProviderHealthIndicator implements HealthIndicator {

    private final FootballDataProvider footballProvider;

    public FootballProviderHealthIndicator(FootballDataProvider footballProvider) {
        this.footballProvider = footballProvider;
    }

    @Override
    public Health health() {
        try {
            List<com.example.football.footballprovider.model.Competition> competitions = footballProvider.getCompetitions();
            if (competitions != null && !competitions.isEmpty()) {
                return Health.up()
                        .withDetail("provider", "sportmonks")
                        .withDetail("competitions", competitions.size())
                        .build();
            }
            return Health.down()
                    .withDetail("provider", "sportmonks")
                    .withDetail("error", "No competitions returned")
                    .build();
        } catch (Exception e) {
            return Health.down()
                    .withDetail("provider", "sportmonks")
                    .withDetail("error", e.getMessage())
                    .build();
        }
    }
}