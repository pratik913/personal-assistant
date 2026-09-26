package com.personalassistant.dto;

public record NotificationTimingRecommendationResponse(
        Integer currentReminderMinutes,
        Integer recommendedReminderMinutes,
        long executionCount,
        double averageMinutesToStart,
        RecommendationConfidence confidence,
        String reason
) {

    public enum RecommendationConfidence {
        NONE,
        LOW,
        MEDIUM,
        HIGH
    }
}