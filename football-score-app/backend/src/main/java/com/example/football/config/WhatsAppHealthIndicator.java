package com.example.football.config;

import com.example.football.whatsapp.WhatsAppService;
import org.springframework.boot.actuate.health.Health;
import org.springframework.boot.actuate.health.HealthIndicator;
import org.springframework.stereotype.Component;

@Component
public class WhatsAppHealthIndicator implements HealthIndicator {

    private final WhatsAppService whatsAppService;

    public WhatsAppHealthIndicator(WhatsAppService whatsAppService) {
        this.whatsAppService = whatsAppService;
    }

    @Override
    public Health health() {
        try {
            // Just check if service is configured
            if (whatsAppService.getProperties().isEnabled() && 
                whatsAppService.getProperties().getAccessToken() != null) {
                return Health.up()
                        .withDetail("enabled", true)
                        .withDetail("phoneNumberId", whatsAppService.getProperties().getPhoneNumberId())
                        .build();
            }
            return Health.down()
                    .withDetail("enabled", false)
                    .withDetail("error", "WhatsApp not configured")
                    .build();
        } catch (Exception e) {
            return Health.down()
                    .withDetail("error", e.getMessage())
                    .build();
        }
    }
}