package com.personalassistant.dto;

import java.time.Instant;
import java.util.UUID;

public record AiPlanItem(
        UUID taskId,
        String taskTitle,
        Instant startAt,
        Instant endAt,
        String reason
) {}