package com.personalassistant.dto;

import java.time.Instant;
import java.util.UUID;

public record AiPlanResponse(
        UUID id,
        String summary,
        AiPlanData plan,
        Instant createdAt,
        Instant updatedAt
) {}