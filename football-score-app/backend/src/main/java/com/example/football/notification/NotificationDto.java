package com.example.football.notification;

import java.time.OffsetDateTime;
import java.util.UUID;

public record NotificationDto(
        UUID id,
        UUID matchId,
        String type,
        String templateName,
        String status,
        OffsetDateTime scheduledFor,
        OffsetDateTime sentAt,
        OffsetDateTime deliveredAt,
        OffsetDateTime createdAt
) {}