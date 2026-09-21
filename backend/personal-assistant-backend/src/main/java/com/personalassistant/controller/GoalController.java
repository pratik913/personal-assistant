package com.personalassistant.controller;

import com.personalassistant.dto.CreateGoalRequest;
import com.personalassistant.dto.GoalResponse;
import com.personalassistant.dto.TaskResponse;
import com.personalassistant.dto.UpdateGoalRequest;
import com.personalassistant.service.GoalService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/goals")
public class GoalController {

    private final GoalService goalService;

    public GoalController(GoalService goalService) {
        this.goalService = goalService;
    }

    @PostMapping
    public ResponseEntity<GoalResponse> createGoal(
            @Valid @RequestBody CreateGoalRequest request,
            Authentication authentication
    ) {
        UUID userId = UUID.fromString(authentication.getName());

        GoalResponse response =
                goalService.createGoal(userId, request);

        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(response);
    }

    @GetMapping
    public ResponseEntity<List<GoalResponse>> getGoals(
            Authentication authentication
    ) {
        UUID userId = UUID.fromString(authentication.getName());

        List<GoalResponse> goals =
                goalService.getGoals(userId);

        return ResponseEntity.ok(goals);
    }

    @GetMapping("/{goalId}")
    public ResponseEntity<GoalResponse> getGoal(
            @PathVariable UUID goalId,
            Authentication authentication
    ) {
        UUID userId = UUID.fromString(authentication.getName());

        GoalResponse response =
                goalService.getGoal(userId, goalId);

        return ResponseEntity.ok(response);
    }

    /*
     * Get all tasks belonging to a goal.
     */
    @GetMapping("/{goalId}/tasks")
    public ResponseEntity<List<TaskResponse>> getTasksByGoal(
            @PathVariable UUID goalId,
            Authentication authentication
    ) {
        UUID userId = UUID.fromString(authentication.getName());

        List<TaskResponse> tasks =
                goalService.getTasksByGoal(userId, goalId);

        return ResponseEntity.ok(tasks);
    }

    /*
     * Get progress information for a goal.
     */
    @GetMapping("/{goalId}/progress")
    public ResponseEntity<GoalService.GoalProgress> getGoalProgress(
            @PathVariable UUID goalId,
            Authentication authentication
    ) {
        UUID userId = UUID.fromString(authentication.getName());

        GoalService.GoalProgress progress =
                goalService.getGoalProgress(userId, goalId);

        return ResponseEntity.ok(progress);
    }

    @PatchMapping("/{goalId}")
    public ResponseEntity<GoalResponse> updateGoal(
            @PathVariable UUID goalId,
            @Valid @RequestBody UpdateGoalRequest request,
            Authentication authentication
    ) {
        UUID userId = UUID.fromString(authentication.getName());

        GoalResponse response =
                goalService.updateGoal(
                        userId,
                        goalId,
                        request
                );

        return ResponseEntity.ok(response);
    }

    @DeleteMapping("/{goalId}")
    public ResponseEntity<Void> deleteGoal(
            @PathVariable UUID goalId,
            Authentication authentication
    ) {
        UUID userId = UUID.fromString(authentication.getName());

        goalService.deleteGoal(userId, goalId);

        return ResponseEntity.noContent().build();
    }
}