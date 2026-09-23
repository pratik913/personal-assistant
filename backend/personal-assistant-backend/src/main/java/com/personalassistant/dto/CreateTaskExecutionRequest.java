package com.personalassistant.dto;

import jakarta.validation.constraints.Size;

public record CreateTaskExecutionRequest(

        @Size(
                max = 2000,
                message = "Feedback must not exceed 2000 characters"
        )
        String feedback
) {
}