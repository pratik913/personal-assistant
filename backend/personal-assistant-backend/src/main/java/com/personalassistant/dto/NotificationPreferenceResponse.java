package com.personalassistant.dto;

public record NotificationPreferenceResponse(
        boolean taskStartNotificationsEnabled,
        Integer reminderMinutes
) {
}