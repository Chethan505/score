package com.example.football.config;

import io.github.resilience4j.circuitbreaker.CircuitBreakerConfig;
import io.github.resilience4j.ratelimiter.RateLimiterConfig;
import io.github.resilience4j.retry.RetryConfig;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.time.Duration;

@Configuration
public class Resilience4jConfig {

    @Value("${whatsapp.retry.max-attempts:4}")
    private int maxAttempts;

    @Value("${whatsapp.retry.base-delay-ms:5000}")
    private long baseDelayMs;

    @Value("${whatsapp.retry.max-delay-ms:60000}")
    private long maxDelayMs;

    @Value("${whatsapp.rate-limit.per-second:80}")
    private int rateLimitPerSecond;

    @Bean
    public RetryConfig whatsappRetryConfig() {
        return RetryConfig.custom()
                .maxAttempts(maxAttempts)
                .waitDuration(Duration.ofMillis(baseDelayMs))
                .exponentialBackoffMultiplier(2.0)
                .maxWaitDuration(Duration.ofMillis(maxDelayMs))
                .retryExceptions(
                        java.net.ConnectException.class,
                        java.net.SocketTimeoutException.class,
                        org.springframework.web.reactive.function.client.WebClientResponseException.class
                )
                .ignoreExceptions(
                        com.example.football.whatsapp.WhatsAppService.WhatsAppException.class
                )
                .build();
    }

    @Bean
    public CircuitBreakerConfig whatsappCircuitBreakerConfig() {
        return CircuitBreakerConfig.custom()
                .failureRateThreshold(50)
                .waitDurationInOpenState(Duration.ofSeconds(30))
                .slidingWindowSize(10)
                .minimumNumberOfCalls(5)
                .permittedNumberOfCallsInHalfOpenState(3)
                .build();
    }

    @Bean
    public RateLimiterConfig whatsappRateLimiterConfig() {
        return RateLimiterConfig.custom()
                .limitForPeriod(rateLimitPerSecond)
                .limitRefreshPeriod(Duration.ofSeconds(1))
                .timeoutDuration(Duration.ofSeconds(10))
                .build();
    }
}