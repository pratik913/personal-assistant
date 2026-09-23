package com.personalassistant.controller;

import com.personalassistant.dto.CreateTaskExecutionRequest;
import com.personalassistant.dto.TaskExecutionResponse;
import com.personalassistant.dto.UpdateTaskExecutionRequest;
import com.personalassistant.service.TaskExecutionService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/tasks/{taskId}/executions")
public class TaskExecutionController {

    private final TaskExecutionService taskExecutionService;

    public TaskExecutionController(
            TaskExecutionService taskExecutionService
    ) {
        this.taskExecutionService = taskExecutionService;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public TaskExecutionResponse startExecution(
            @PathVariable UUID taskId,
            @Valid @RequestBody(required = false)
            CreateTaskExecutionRequest request,
            Authentication authentication
    ) {

        UUID userId = UUID.fromString(authentication.getName());

        return taskExecutionService.startExecution(
                taskId,
                userId,
                request
        );
    }

    @PatchMapping("/{executionId}")
    public TaskExecutionResponse updateExecution(
            @PathVariable UUID taskId,
            @PathVariable UUID executionId,
            @Valid @RequestBody UpdateTaskExecutionRequest request,
            Authentication authentication
    ) {

        UUID userId = UUID.fromString(authentication.getName());

        return taskExecutionService.updateExecution(
                taskId,
                executionId,
                userId,
                request
        );
    }

    @GetMapping
    public List<TaskExecutionResponse> getExecutions(
            @PathVariable UUID taskId,
            Authentication authentication
    ) {

        UUID userId = UUID.fromString(authentication.getName());

        return taskExecutionService.getExecutions(
                taskId,
                userId
        );
    }
}