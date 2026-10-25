package com.example.football.footballprovider;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class FootballProviderConfig {

    @Value("${football.provider:sportmonks}")
    private String providerName;

    @Autowired(required = false)
    private SportmonksProvider sportmonksProvider;

    @Bean
    public FootballDataProvider footballDataProvider() {
        return switch (providerName.toLowerCase()) {
            case "sportmonks" -> sportmonksProvider;
            default -> throw new IllegalArgumentException("Unknown football provider: " + providerName);
        };
    }
}