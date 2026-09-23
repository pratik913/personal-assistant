package com.personalassistant.dto;

import com.personalassistant.entity.TaskExecutionStatus;
import jakarta.validation.constraints.Size;

public record UpdateTaskExecutionRequest(

        TaskExecutionStatus status,

        @Size(
                max = 2000,
                message = "Feedback must not exceed 2000 characters"
        )
        String feedback
) {
}