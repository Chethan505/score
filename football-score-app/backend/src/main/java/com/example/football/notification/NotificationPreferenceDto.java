package com.example.football.notification;

import java.time.LocalTime;

public record NotificationPreferenceDto(
        boolean matchStartReminder,
        boolean goalNotifications,
        boolean cardNotifications,
        boolean substitutionNotifications,
        boolean halfTimeNotifications,
        boolean fullTimeNotifications,
        int reminderMinutesBefore,
        LocalTime quietHoursStart,
        LocalTime quietHoursEnd
) {}