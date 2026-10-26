package com.example.football.footballprovider.sportmonks.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

@JsonIgnoreProperties(ignoreUnknown = true)
public record RateLimit(
        @JsonProperty("resets_in_seconds") Integer resetsInSeconds,
        @JsonProperty("remaining") Integer remaining,
        @JsonProperty("requested_entity") String requestedEntity
) {}
