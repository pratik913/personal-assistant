package com.personalassistant.dto;

import com.personalassistant.entity.TaskPriority;
import com.personalassistant.entity.TaskStatus;
import lombok.Getter;
import lombok.Setter;

import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

@Getter
@Setter
public class TaskResponse {

    private UUID id;

    private String title;

    private String description;

    private TaskStatus status;

    private TaskPriority priority;

    private LocalDate dueDate;

    private Integer estimatedMinutes;

    private Instant createdAt;

    private Instant updatedAt;

    private UUID captureId;
}