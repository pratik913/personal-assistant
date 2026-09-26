package com.personalassistant.controller;

import com.personalassistant.dto.TaskPlanningInsightResponse;
import com.personalassistant.service.TaskPlanningService;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/tasks")
public class TaskPlanningController {

    private final TaskPlanningService taskPlanningService;

    public TaskPlanningController(
            TaskPlanningService taskPlanningService
    ) {
        this.taskPlanningService = taskPlanningService;
    }

    @GetMapping("/{taskId}/planning-insight")
    public TaskPlanningInsightResponse getPlanningInsight(
            @PathVariable UUID taskId,
            Authentication authentication
    ) {

        UUID userId =
                UUID.fromString(authentication.getName());

        return taskPlanningService.getPlanningInsight(
                taskId,
                userId
        );
    }
}