package com.personalassistant.dto;

import com.personalassistant.entity.TaskExecutionStatus;

import java.time.Instant;
import java.util.UUID;

public record TaskExecutionResponse(
        UUID id,
        UUID taskId,
        Instant startedAt,
        Instant endedAt,
        TaskExecutionStatus status,
        String feedback,
        Instant createdAt
) {
}