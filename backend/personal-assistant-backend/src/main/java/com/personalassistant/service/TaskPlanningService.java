package com.personalassistant.service;

import com.personalassistant.dto.PlanningConfidence;
import com.personalassistant.dto.TaskPlanningInsightResponse;
import com.personalassistant.entity.Task;
import com.personalassistant.entity.TaskExecution;
import com.personalassistant.entity.TaskExecutionStatus;
import com.personalassistant.exception.TaskNotFoundException;
import com.personalassistant.repository.TaskExecutionRepository;
import com.personalassistant.repository.TaskRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.util.List;
import java.util.UUID;

@Service
@Transactional(readOnly = true)
public class TaskPlanningService {

    private static final long MAX_PLANNING_DURATION_SECONDS =
            8 * 60 * 60;

    private final TaskRepository taskRepository;
    private final TaskExecutionRepository taskExecutionRepository;

    public TaskPlanningService(
            TaskRepository taskRepository,
            TaskExecutionRepository taskExecutionRepository
    ) {
        this.taskRepository = taskRepository;
        this.taskExecutionRepository = taskExecutionRepository;
    }

    public TaskPlanningInsightResponse getPlanningInsight(
            UUID taskId,
            UUID userId
    ) {

        Task task = taskRepository
                .findByIdAndUserId(taskId, userId)
                .orElseThrow(() ->
                        new TaskNotFoundException(
                                "Task not found"
                        )
                );

        List<TaskExecution> completedExecutions =
                taskExecutionRepository
                        .findByTaskIdAndUserIdAndStatusOrderByStartedAtDesc(
                                taskId,
                                userId,
                                TaskExecutionStatus.COMPLETED
                        );

        long executionCount =
                completedExecutions.size();

        List<TaskExecution> planningExecutions =
                completedExecutions.stream()
                        .filter(this::isEligibleForPlanning)
                        .toList();

        long planningExecutionCount =
                planningExecutions.size();

        long excludedExecutionCount =
                executionCount - planningExecutionCount;

        if (planningExecutionCount == 0) {

            return new TaskPlanningInsightResponse(
                    task.getId(),
                    task.getEstimatedMinutes(),
                    null,
                    executionCount,
                    0,
                    excludedExecutionCount,
                    task.getEstimatedMinutes(),
                    PlanningConfidence.NONE
            );
        }

        double averageActualMinutes =
                calculateAverageActualMinutes(
                        planningExecutions
                );

        PlanningConfidence confidence =
                calculateConfidence(
                        planningExecutionCount
                );

        Integer recommendedMinutes =
                calculateRecommendedMinutes(
                        task.getEstimatedMinutes(),
                        averageActualMinutes,
                        confidence
                );

        return new TaskPlanningInsightResponse(
                task.getId(),
                task.getEstimatedMinutes(),
                averageActualMinutes,
                executionCount,
                planningExecutionCount,
                excludedExecutionCount,
                recommendedMinutes,
                confidence
        );
    }

    /**
     * Only completed sessions with a sensible duration
     * participate in planning intelligence.
     *
     * Extremely long sessions are kept in history but
     * excluded from planning calculations.
     */
    private boolean isEligibleForPlanning(
            TaskExecution execution
    ) {

        if (execution.getStartedAt() == null
                || execution.getEndedAt() == null) {

            return false;
        }

        long durationSeconds =
                Duration.between(
                        execution.getStartedAt(),
                        execution.getEndedAt()
                ).getSeconds();

        return durationSeconds > 0
                && durationSeconds <= MAX_PLANNING_DURATION_SECONDS;
    }

    private double calculateAverageActualMinutes(
            List<TaskExecution> executions
    ) {

        long totalSeconds = 0;

        for (TaskExecution execution : executions) {

            totalSeconds += Duration.between(
                    execution.getStartedAt(),
                    execution.getEndedAt()
            ).getSeconds();
        }

        if (totalSeconds == 0) {
            return 0.0;
        }

        return totalSeconds / 60.0 / executions.size();
    }

    /**
     * Recommendation becomes increasingly dependent on
     * historical behaviour as more execution data becomes
     * available.
     *
     * LOW:
     * 75% current estimate + 25% historical average
     *
     * MEDIUM:
     * 40% current estimate + 60% historical average
     *
     * HIGH:
     * 100% historical average
     */
    private int calculateRecommendedMinutes(
            Integer estimatedMinutes,
            double averageActualMinutes,
            PlanningConfidence confidence
    ) {

        if (averageActualMinutes <= 0) {

            return estimatedMinutes != null
                    ? Math.max(1, estimatedMinutes)
                    : 1;
        }

        if (estimatedMinutes == null
                || estimatedMinutes <= 0) {

            return Math.max(
                    1,
                    (int) Math.round(
                            averageActualMinutes
                    )
            );
        }

        double recommendation;

        switch (confidence) {

            case LOW:

                recommendation =
                        (estimatedMinutes * 0.75)
                                + (averageActualMinutes * 0.25);

                break;

            case MEDIUM:

                recommendation =
                        (estimatedMinutes * 0.40)
                                + (averageActualMinutes * 0.60);

                break;

            case HIGH:

                recommendation =
                        averageActualMinutes;

                break;

            case NONE:

            default:

                recommendation =
                        estimatedMinutes;
        }

        return Math.max(
                1,
                (int) Math.round(recommendation)
        );
    }

    private PlanningConfidence calculateConfidence(
            long planningExecutionCount
    ) {

        if (planningExecutionCount == 0) {
            return PlanningConfidence.NONE;
        }

        if (planningExecutionCount == 1) {
            return PlanningConfidence.LOW;
        }

        if (planningExecutionCount <= 4) {
            return PlanningConfidence.MEDIUM;
        }

        return PlanningConfidence.HIGH;
    }
}