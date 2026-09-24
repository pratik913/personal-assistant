package com.personalassistant.dto;

import java.time.Instant;
import java.util.UUID;

public record ExecutionAnalysisResponse(
        UUID id,
        UUID executionId,
        ExecutionDifficulty difficulty,
        ExecutionTimeAssessment timeAssessment,
        ExecutionBlocker blocker,
        String insight,
        String suggestion,
        Instant createdAt
) {
}