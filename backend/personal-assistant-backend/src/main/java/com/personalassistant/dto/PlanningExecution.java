package com.personalassistant.dto;

public record PlanningExecution(
        long actualSeconds,
        boolean eligible
) {
}