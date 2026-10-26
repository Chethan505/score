package com.example.football.footballprovider.sportmonks.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

@JsonIgnoreProperties(ignoreUnknown = true)
public record Pagination(
        @JsonProperty("current_page") Integer currentPage,
        @JsonProperty("per_page") Integer perPage,
        @JsonProperty("has_more") Boolean hasMore,
        @JsonProperty("next_page") Integer nextPage,
        @JsonProperty("total_pages") Integer totalPages,
        @JsonProperty("total") Integer total
) {}
