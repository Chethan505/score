package com.example.football.whatsapp.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.*;

import java.util.List;
import java.util.Map;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class WhatsAppTemplateMessage {
    private String to;
    private String templateName;
    private String languageCode = "en";
    private Map<String, String> variables;

    public Map<String, Object> toJson() {
        return Map.of(
                "messaging_product", "whatsapp",
                "to", to,
                "type", "template",
                "template", Map.of(
                        "name", templateName,
                        "language", Map.of("code", languageCode),
                        "components", buildComponents()
                )
        );
    }

    private List<Map<String, Object>> buildComponents() {
        if (variables == null || variables.isEmpty()) {
            return List.of();
        }

        return List.of(Map.of(
                "type", "body",
                "parameters", variables.entrySet().stream()
                        .map(e -> Map.of("type", "text", "text", e.getValue()))
                        .toList()
        ));
    }
}


