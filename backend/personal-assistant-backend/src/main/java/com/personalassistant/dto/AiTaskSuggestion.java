package com.personalassistant.dto;

public record AiTaskSuggestion(
        String title,
        String description,
        Integer estimatedMinutes
) {
}