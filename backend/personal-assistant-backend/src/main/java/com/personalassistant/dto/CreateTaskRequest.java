package com.personalassistant.dto;

import com.personalassistant.entity.TaskPriority;
import com.personalassistant.entity.TaskStatus;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDate;
import java.util.UUID;

@Getter
@Setter
public class CreateTaskRequest {

    @NotBlank
    @Size(max = 200)
    private String title;

    @Size(max = 5000)
    private String description;

    private TaskStatus status;

    private TaskPriority priority;

    private LocalDate dueDate;

    @Min(1)
    private Integer estimatedMinutes;

    private UUID captureId;

    private UUID goalId;
}