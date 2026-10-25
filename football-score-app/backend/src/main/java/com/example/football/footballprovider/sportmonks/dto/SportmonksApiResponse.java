package com.example.football.footballprovider.sportmonks.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.List;

@JsonIgnoreProperties(ignoreUnknown = true)
public record SportmonksApiResponse<T>(
        @JsonProperty("data") T data,
        @JsonProperty("meta") Meta meta,
        @JsonProperty("errors") List<Error> errors
) {}

@JsonIgnoreProperties(ignoreUnknown = true)
public record Meta(
        @JsonProperty("pagination") Pagination pagination,
        @JsonProperty("rate_limit") RateLimit rateLimit
) {}

@JsonIgnoreProperties(ignoreUnknown = true)
public record Pagination(
        @JsonProperty("count") int count,
        @JsonProperty("per_page") int perPage,
        @JsonProperty("current_page") int currentPage,
        @JsonProperty("total_pages") int totalPages,
        @JsonProperty("total_count") int totalCount
) {}

@JsonIgnoreProperties(ignoreUnknown = true)
public record RateLimit(
        @JsonProperty("limit") int limit,
        @JsonProperty("remaining") int remaining,
        @JsonProperty("reset") long reset
) {}

@JsonIgnoreProperties(ignoreUnknown = true)
public record Error(
        @JsonProperty("code") String code,
        @JsonProperty("message") String message
) {}