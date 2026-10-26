package com.example.football.footballprovider.sportmonks.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

@JsonIgnoreProperties(ignoreUnknown = true)
public record Meta(
        @JsonProperty("plan") String plan,
        @JsonProperty("sports") java.util.List<String> sports,
        @JsonProperty("pages") Integer pages,
        @JsonProperty("page") Integer page,
        @JsonProperty("per_page") Integer perPage,
        @JsonProperty("count") Integer count
) {}
