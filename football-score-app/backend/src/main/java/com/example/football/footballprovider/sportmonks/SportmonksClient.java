package com.example.football.footballprovider.sportmonks;

import com.example.football.footballprovider.sportmonks.dto.*;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientResponseException;
import reactor.core.publisher.Mono;
import reactor.util.retry.Retry;

import java.time.Duration;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Component
public class SportmonksClient {

    private final WebClient webClient;
    private final SportmonksProperties properties;
    private final Map<String, Object> cache = new ConcurrentHashMap<>();

    public SportmonksClient(@Qualifier("sportmonksWebClient") WebClient webClient, SportmonksProperties properties) {
        this.webClient = webClient;
        this.properties = properties;
    }

    public List<SportmonksLeague> getLeagues() {
        return getWithCache("leagues", () -> webClient.get()
                .uri(uriBuilder -> uriBuilder
                        .path("/leagues")
                        .queryParam("include", "country;season")
                        .queryParam("per_page", 500)
                        .build())
                .retrieve()
                .bodyToMono(SportmonksApiResponse.class)
                .retryWhen(Retry.backoff(properties.getMaxRetries(), Duration.ofSeconds(2))
                        .filter(throwable -> throwable instanceof WebClientResponseException
                                && ((WebClientResponseException) throwable).getStatusCode().is5xxServerError()))
                .block())
                .data();
    }

    public List<SportmonksSeason> getSeasons(int leagueId) {
        String cacheKey = "seasons:" + leagueId;
        return getWithCache(cacheKey, () -> webClient.get()
                .uri(uriBuilder -> uriBuilder
                        .path("/leagues/{leagueId}/seasons")
                        .build(leagueId))
                .retrieve()
                .bodyToMono(SportmonksApiResponse.class)
                .retryWhen(Retry.backoff(properties.getMaxRetries(), Duration.ofSeconds(2)))
                .block())
                .data();
    }

    public List<SportmonksTeam> getTeams(int leagueId, int seasonId) {
        String cacheKey = "teams:" + leagueId + ":" + seasonId;
        return getWithCache(cacheKey, () -> webClient.get()
                .uri(uriBuilder -> uriBuilder
                        .path("/leagues/{leagueId}/seasons/{seasonId}/teams")
                        .build(leagueId, seasonId))
                .retrieve()
                .bodyToMono(SportmonksApiResponse.class)
                .retryWhen(Retry.backoff(properties.getMaxRetries(), Duration.ofSeconds(2)))
                .block())
                .data();
    }

    public List<SportmonksFixture> getFixtures(int leagueId, int seasonId, String from, String to) {
        return webClient.get()
                .uri(uriBuilder -> uriBuilder
                        .path("/leagues/{leagueId}/seasons/{seasonId}/fixtures")
                        .queryParam("include", "participants;scores;events;lineups;statistics")
                        .queryParam("filters", "startingAtBetween:" + from + "|" + to)
                        .queryParam("per_page", 500)
                        .build(leagueId, seasonId))
                .retrieve()
                .bodyToMono(SportmonksApiResponse.class)
                .retryWhen(Retry.backoff(properties.getMaxRetries(), Duration.ofSeconds(2))
                        .filter(throwable -> throwable instanceof WebClientResponseException
                                && ((WebClientResponseException) throwable).getStatusCode().is5xxServerError()))
                .block()
                .data();
    }

    public List<SportmonksFixture> getLiveFixtures() {
        return webClient.get()
                .uri(uriBuilder -> uriBuilder
                        .path("/fixtures/live")
                        .queryParam("include", "participants;scores;events;lineups;statistics")
                        .queryParam("per_page", 500)
                        .build())
                .retrieve()
                .bodyToMono(SportmonksApiResponse.class)
                .retryWhen(Retry.backoff(properties.getMaxRetries(), Duration.ofSeconds(2)))
                .block()
                .data();
    }

    public SportmonksFixture getFixtureById(long fixtureId) {
        return webClient.get()
                .uri(uriBuilder -> uriBuilder
                        .path("/fixtures/{fixtureId}")
                        .queryParam("include", "participants;scores;events;lineups;statistics")
                        .build(fixtureId))
                .retrieve()
                .bodyToMono(SportmonksApiResponse.class)
                .retryWhen(Retry.backoff(properties.getMaxRetries(), Duration.ofSeconds(2)))
                .block()
                .data()
                .stream()
                .findFirst()
                .orElse(null);
    }

    public List<SportmonksStanding> getStandings(int leagueId, int seasonId) {
        return webClient.get()
                .uri(uriBuilder -> uriBuilder
                        .path("/leagues/{leagueId}/seasons/{seasonId}/standings")
                        .queryParam("per_page", 50)
                        .build(leagueId, seasonId))
                .retrieve()
                .bodyToMono(SportmonksApiResponse.class)
                .retryWhen(Retry.backoff(properties.getMaxRetries(), Duration.ofSeconds(2)))
                .block()
                .data();
    }

    @SuppressWarnings("unchecked")
    private <T> T getWithCache(String key, java.util.function.Supplier<T> supplier) {
        return (T) cache.computeIfAbsent(key, k -> supplier.get());
    }

    public void clearCache() {
        cache.clear();
    }

    public void evictCache(String key) {
        cache.remove(key);
    }
}