package com.personalassistant.dto;

import com.personalassistant.entity.TaskPriority;

import java.time.LocalDate;

public record AiTaskSuggestion(
        String title,
        String description,
        Integer estimatedMinutes,
        TaskPriority priority,
        LocalDate dueDate
) {
}