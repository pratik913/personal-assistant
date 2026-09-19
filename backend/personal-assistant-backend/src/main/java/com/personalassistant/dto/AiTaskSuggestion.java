package com.personalassistant.dto;

import com.personalassistant.entity.TaskPriority;

public record AiTaskSuggestion(
        String title,
        String description,
        Integer estimatedMinutes,
        TaskPriority priority
) {}