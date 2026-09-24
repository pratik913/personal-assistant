package com.personalassistant.dto;

public record ExecutionAnalysis(
        ExecutionDifficulty difficulty,
        ExecutionTimeAssessment timeAssessment,
        ExecutionBlocker blocker,
        String insight,
        String suggestion
) {
}