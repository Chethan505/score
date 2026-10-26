package com.example.football.whatsapp.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
public class WhatsAppErrorResponse {
    @JsonProperty(\"error\")
    private Error error;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class Error {
        @JsonProperty(\"message\")
        private String message;
        @JsonProperty(\"type\")
        private String type;
        @JsonProperty(\"code\")
        private int code;
        @JsonProperty(\"error_subcode\")
        private int errorSubcode;
        @JsonProperty(\"fbtrace_id\")
        private String fbtraceId;
    }
}
