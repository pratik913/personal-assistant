package com.personalassistant.dto;


import java.time.Instant;
import java.util.UUID;

public record ScheduleEntryResponse(
        UUID id,
        UUID taskId,
        String taskTitle,
        Instant startAt,
        Instant endAt,
        Instant createdAt,
        Instant updatedAt
) {
}