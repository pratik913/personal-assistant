package com.personalassistant.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

import java.time.LocalDate;

public record CreateGoalRequest(

        @NotBlank(message = "Goal title is required")
        @Size(max = 200, message = "Goal title must not exceed 200 characters")
        String title,

        @Size(max = 5000, message = "Goal description must not exceed 5000 characters")
        String description,

        LocalDate targetDate
) {
}