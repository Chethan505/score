package com.example.football.footballprovider.sportmonks.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

@JsonIgnoreProperties(ignoreUnknown = true)
public record Error(
        @JsonProperty("code") String code,
        @JsonProperty("message") String message,
        @JsonProperty("message_en") String messageEn
) {}
