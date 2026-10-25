package com.example.football.whatsapp;

import com.example.football.whatsapp.dto.WhatsAppWebhookPayload;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/webhook/whatsapp")
public class WhatsAppWebhookController {

    private static final Logger log = LoggerFactory.getLogger(WhatsAppWebhookController.class);

    private final WhatsAppService whatsAppService;
    private final NotificationRepository notificationRepository;

    public WhatsAppWebhookController(WhatsAppService whatsAppService, NotificationRepository notificationRepository) {
        this.whatsAppService = whatsAppService;
        this.notificationRepository = notificationRepository;
    }

    @GetMapping
    public ResponseEntity<String> verifyWebhook(
            @RequestParam("hub.mode") String mode,
            @RequestParam("hub.challenge") String challenge,
            @RequestParam("hub.verify_token") String verifyToken
    ) {
        if ("subscribe".equals(mode) && verifyToken.equals(whatsAppService.getProperties().getWebhookVerifyToken())) {
            log.info("WhatsApp webhook verified successfully");
            return ResponseEntity.ok(challenge);
        }
        log.warn("WhatsApp webhook verification failed");
        return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
    }

    @PostMapping
    public ResponseEntity<Void> receiveWebhook(
            @RequestHeader("X-Hub-Signature-256") String signature,
            @RequestBody String payload
    ) {
        try {
            whatsAppService.verifyWebhookSignature(payload, signature);
            whatsAppService.processWebhook(
                    com.fasterxml.jackson.databind.ObjectMapper.class.cast(
                            new com.fasterxml.jackson.databind.ObjectMapper().readValue(payload, WhatsAppWebhookPayload.class)
                    )
            );
            return ResponseEntity.ok().build();
        } catch (SecurityException e) {
            log.warn("Invalid WhatsApp webhook signature");
            return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
        } catch (Exception e) {
            log.error("Error processing WhatsApp webhook", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }
}