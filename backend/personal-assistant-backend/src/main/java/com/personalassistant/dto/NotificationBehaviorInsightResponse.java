package com.personalassistant.dto;

public record NotificationBehaviorInsightResponse(
        long totalReminders,
        long readReminders,
        long ignoredReminders,
        long actedOnReminders,
        double readRate,
        double actionRate,
        double averageMinutesToStart,
        String insight
) {
}