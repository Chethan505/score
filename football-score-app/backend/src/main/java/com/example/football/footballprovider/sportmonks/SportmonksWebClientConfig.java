package com.example.football.footballprovider.sportmonks;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.web.reactive.function.client.WebClient;

@Configuration
public class SportmonksWebClientConfig {

    @Autowired
    private SportmonksProperties properties;

    @Bean
    @org.springframework.context.annotation.Primary
    public WebClient webClient(WebClient.Builder builder) {
        return builder
                .baseUrl(properties.getBaseUrl())
                .defaultHeader(HttpHeaders.AUTHORIZATION, "Bearer " + properties.getToken())
                .defaultHeader(HttpHeaders.ACCEPT, MediaType.APPLICATION_JSON_VALUE)
                .defaultHeader(HttpHeaders.USER_AGENT, "FootballScoreApp/1.0")
                .codecs(configurer -> configurer.defaultCodecs().maxInMemorySize(10 * 1024 * 1024))
                .build();
    }

    @Bean
    @Qualifier("sportmonksWebClient")
    public WebClient sportmonksWebClient(WebClient.Builder builder) {
        return webClient(builder);
    }
}