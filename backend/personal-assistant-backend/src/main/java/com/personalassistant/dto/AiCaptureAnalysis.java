package com.personalassistant.dto;

import java.util.List;

public record AiCaptureAnalysis(
        String summary,
        List<AiTaskSuggestion> tasks
) {
}