package com.personalassistant.dto;

import com.personalassistant.entity.TaskPriority;
import com.personalassistant.entity.TaskStatus;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

import java.time.Instant;

@Getter
@Setter
public class UpdateTaskRequest {

    @Size(max = 200)
    private String title;

    private String description;

    private TaskStatus status;

    private TaskPriority priority;

    private Instant dueDate;

    @Min(1)
    private Integer estimatedMinutes;
}