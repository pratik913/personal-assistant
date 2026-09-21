package com.personalassistant.dto;

import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

public record GoalResponse(

        UUID id,

        String title,

        String description,

        LocalDate targetDate,

        Instant createdAt,

        Instant updatedAt
) {
}