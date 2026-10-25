package com.example.football.whatsapp;

import com.example.football.whatsapp.dto.WhatsAppErrorResponse;
import com.example.football.whatsapp.dto.WhatsAppSendResponse;
import com.example.football.whatsapp.dto.WhatsAppTemplateMessage;
import com.example.football.whatsapp.dto.WhatsAppWebhookPayload;
import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import io.github.resilience4j.ratelimiter.annotation.RateLimiter;
import io.github.resilience4j.retry.annotation.Retry;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientResponseException;
import reactor.core.publisher.Mono;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.HexFormat;

@Service
public class WhatsAppService {

    private static final Logger log = LoggerFactory.getLogger(WhatsAppService.class);

    private final WhatsAppProperties properties;
    private final WebClient webClient;

    public WhatsAppService(WhatsAppProperties properties, WebClient.Builder webClientBuilder) {
        this.properties = properties;
        this.webClient = webClientBuilder
                .baseUrl(properties.getBaseUrl())
                .defaultHeader(HttpHeaders.AUTHORIZATION, "Bearer " + properties.getAccessToken())
                .defaultHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                .build();
    }

    @Retry(name = "whatsapp", fallbackMethod = "sendTemplateMessageFallback")
    @CircuitBreaker(name = "whatsapp")
    @RateLimiter(name = "whatsapp")
    public String sendTemplateMessage(WhatsAppTemplateMessage message) {
        if (!properties.isEnabled()) {
            log.info("WhatsApp disabled, simulating send for: {}", message.getTo());
            return "simulated_" + System.currentTimeMillis();
        }

        log.debug("Sending WhatsApp template {} to {}", message.getTemplateName(), message.getTo());

        WhatsAppSendResponse response = webClient.post()
                .uri("/{phoneNumberId}/messages", properties.getPhoneNumberId())
                .bodyValue(message.toJson())
                .retrieve()
                .bodyToMono(WhatsAppSendResponse.class)
                .block();

        if (response != null && response.getMessages() != null && !response.getMessages().isEmpty()) {
            String messageId = response.getMessages().get(0).getId();
            log.info("WhatsApp message sent successfully: {} to {}", messageId, message.getTo());
            return messageId;
        }

        throw new WhatsAppException("Failed to send WhatsApp message: no message ID in response");
    }

    private String sendTemplateMessageFallback(WhatsAppTemplateMessage message, Exception ex) {
        log.error("Failed to send WhatsApp message to {} after retries", message.getTo(), ex);
        throw new WhatsAppException("Failed to send WhatsApp message after retries", ex);
    }

    public void verifyWebhookSignature(String payload, String signature) {
        if (properties.getAppSecret() == null) {
            log.warn("WhatsApp app secret not configured, skipping signature verification");
            return;
        }

        try {
            Mac mac = Mac.getInstance("HmacSHA256");
            mac.init(new SecretKeySpec(properties.getAppSecret().getBytes(StandardCharsets.UTF_8), "HmacSHA256"));
            byte[] hash = mac.doFinal(payload.getBytes(StandardCharsets.UTF_8));
            String expectedSignature = "sha256=" + HexFormat.of().formatHex(hash);

            if (!expectedSignature.equals(signature)) {
                throw new SecurityException("Invalid WhatsApp webhook signature");
            }
        } catch (Exception e) {
            log.error("Error verifying WhatsApp webhook signature", e);
            throw new SecurityException("Failed to verify webhook signature", e);
        }
    }

    public void processWebhook(WhatsAppWebhookPayload payload) {
        if (payload.getEntry() == null) {
            return;
        }

        for (WhatsAppWebhookPayload.Entry entry : payload.getEntry()) {
            if (entry.getChanges() == null) continue;

            for (WhatsAppWebhookPayload.Change change : entry.getChanges()) {
                if (change.getValue() == null) continue;

                WhatsAppWebhookPayload.Value value = change.getValue();

                if (value.getStatuses() != null) {
                    for (WhatsAppWebhookPayload.Status status : value.getStatuses()) {
                        handleStatusUpdate(status);
                    }
                }

                if (value.getMessages() != null) {
                    for (WhatsAppWebhookPayload.Message message : value.getMessages()) {
                        handleIncomingMessage(message);
                    }
                }
            }
        }
    }

    private void handleStatusUpdate(WhatsAppWebhookPayload.Status status) {
        log.info("WhatsApp status update: {} -> {} for message {}", 
                status.getId(), status.getStatus(), status.getRecipientId());
        // Status updates are handled by NotificationWorker via webhook controller
    }

    private void handleIncomingMessage(WhatsAppWebhookPayload.Message message) {
        log.info("Incoming WhatsApp message from {}: {}", message.getFrom(), 
                message.getText() != null ? message.getText().getBody() : "non-text");
        // Handle opt-in, STOP, etc.
    }

    public boolean isValidPhoneNumber(String phoneNumber) {
        return phoneNumber != null && phoneNumber.matches("^\\+?[1-9]\\d{1,14}$");
    }

    public static class WhatsAppException extends RuntimeException {
        public WhatsAppException(String message) {
            super(message);
        }

        public WhatsAppException(String message, Throwable cause) {
            super(message, cause);
        }
    }
}