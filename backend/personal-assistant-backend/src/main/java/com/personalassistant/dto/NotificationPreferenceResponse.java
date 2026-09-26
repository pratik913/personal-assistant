package com.personalassistant.dto;

import java.time.LocalTime;

public record NotificationPreferenceResponse(
        boolean taskStartNotificationsEnabled,
        Integer reminderMinutes,
        boolean quietHoursEnabled,
        LocalTime quietHoursStart,
        LocalTime quietHoursEnd
) {
}