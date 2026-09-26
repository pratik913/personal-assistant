package com.personalassistant.dto;

import java.util.UUID;

public record TaskPlanningInsightResponse(

        UUID taskId,

        Integer estimatedMinutes,

        Double averageActualMinutes,

        long executionCount,

        long planningExecutionCount,

        long excludedExecutionCount,

        Integer recommendedMinutes,

        PlanningConfidence confidence

) {
}