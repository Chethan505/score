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
public class WhatsAppSendResponse {
    @JsonProperty(\"contacts\")
    private java.util.List<Contact> contacts;

    @JsonProperty(\"messages\")
    private java.util.List<Message> messages;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class Contact {
        @JsonProperty(\"input\")
        private String input;
        @JsonProperty(\"wa_id\")
        private String waId;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class Message {
        @JsonProperty(\"id\")
        private String id;
        @JsonProperty(\"message_status\")
        private String messageStatus;
    }
}
