package com.example.football.notification;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/notifications")
public class NotificationController {

    private final NotificationService notificationService;

    public NotificationController(NotificationService notificationService) {
        this.notificationService = notificationService;
    }

    @GetMapping
    public ResponseEntity<List<NotificationDto>> getNotifications(@CurrentUser UUID userId) {
        return ResponseEntity.ok(notificationService.getNotifications(userId));
    }

    @GetMapping("/preferences")
    public ResponseEntity<NotificationPreferenceDto> getPreferences(@CurrentUser UUID userId) {
        return ResponseEntity.ok(notificationService.getPreferences(userId));
    }

    @PutMapping("/preferences")
    public ResponseEntity<NotificationPreferenceDto> updatePreferences(
            @CurrentUser UUID userId,
            @RequestBody NotificationPreferenceDto preferences
    ) {
        return ResponseEntity.ok(notificationService.updatePreferences(userId, preferences));
    }
}