package com.example.football.whatsapp.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
public class WhatsAppWebhookPayload {
    @JsonProperty(\"object\")
    private String object;

    @JsonProperty(\"entry\")
    private List<Entry> entry;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class Entry {
        @JsonProperty(\"id\")
        private String id;
        @JsonProperty(\"changes\")
        private List<Change> changes;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class Change {
        @JsonProperty(\"value\")
        private Value value;
        @JsonProperty(\"field\")
        private String field;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class Value {
        @JsonProperty(\"messaging_product\")
        private String messagingProduct;
        @JsonProperty(\"metadata\")
        private Metadata metadata;
        @JsonProperty(\"statuses\")
        private List<Status> statuses;
        @JsonProperty(\"messages\")
        private List<Message> messages;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class Metadata {
        @JsonProperty(\"display_phone_number\")
        private String displayPhoneNumber;
        @JsonProperty(\"phone_number_id\")
        private String phoneNumberId;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class Status {
        @JsonProperty(\"id\")
        private String id;
        @JsonProperty(\"status\")
        private String status;
        @JsonProperty(\"timestamp\")
        private String timestamp;
        @JsonProperty(\"recipient_id\")
        private String recipientId;
        @JsonProperty(\"conversation\")
        private Conversation conversation;
        @JsonProperty(\"pricing\")
        private Pricing pricing;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class Conversation {
        @JsonProperty(\"id\")
        private String id;
        @JsonProperty(\"expiration_timestamp\")
        private String expirationTimestamp;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class Pricing {
        @JsonProperty(\"billable\")
        private boolean billable;
        @JsonProperty(\"category\")
        private String category;
        @JsonProperty(\"pricing_model\")
        private String pricingModel;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class Message {
        @JsonProperty(\"from\")
        private String from;
        @JsonProperty(\"id\")
        private String id;
        @JsonProperty(\"timestamp\")
        private String timestamp;
        @JsonProperty(\"type\")
        private String type;
        @JsonProperty(\"text\")
        private Text text;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class Text {
        @JsonProperty(\"body\")
        private String body;
    }
}
