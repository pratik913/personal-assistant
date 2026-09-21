package com.personalassistant.controller;

import com.personalassistant.dto.CreateTaskRequest;
import com.personalassistant.dto.TaskResponse;
import com.personalassistant.dto.UpdateTaskRequest;
import com.personalassistant.service.TaskService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/tasks")
public class TaskController {

    private final TaskService taskService;

    public TaskController(TaskService taskService) {
        this.taskService = taskService;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public TaskResponse createTask(
            @Valid @RequestBody CreateTaskRequest request,
            Authentication authentication
    ) {

        UUID userId = UUID.fromString(authentication.getName());

        return taskService.createTask(request, userId);
    }

    @GetMapping
    public List<TaskResponse> getMyTasks(
            Authentication authentication
    ) {

        UUID userId = UUID.fromString(authentication.getName());

        return taskService.getMyTasks(userId);
    }

    @GetMapping("/{taskId}")
    public TaskResponse getTask(
            @PathVariable UUID taskId,
            Authentication authentication
    ) {

        UUID userId = UUID.fromString(authentication.getName());

        return taskService.getTask(taskId, userId);
    }

    @DeleteMapping("/{taskId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deleteTask(
            @PathVariable UUID taskId,
            Authentication authentication
    ) {

        UUID userId = UUID.fromString(authentication.getName());

        taskService.deleteTask(taskId, userId);
    }

    @PatchMapping("/{taskId}")
    public TaskResponse updateTask(
            @PathVariable UUID taskId,
            @Valid @RequestBody UpdateTaskRequest request,
            Authentication authentication
    ) {

        UUID userId = UUID.fromString(authentication.getName());

        return taskService.updateTask(
                taskId,
                userId,
                request
        );
    }

    @DeleteMapping("/{taskId}/goal")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void removeTaskFromGoal(
            @PathVariable UUID taskId,
            Authentication authentication
    ) {

        UUID userId = UUID.fromString(authentication.getName());

        taskService.removeTaskFromGoal(
                taskId,
                userId
        );
    }

    @GetMapping("/capture/{captureId}")
    public List<TaskResponse> getTasksByCapture(
            @PathVariable UUID captureId,
            Authentication authentication
    ) {

        UUID userId = UUID.fromString(authentication.getName());

        return taskService.getTasksByCapture(
                captureId,
                userId
        );
    }
}