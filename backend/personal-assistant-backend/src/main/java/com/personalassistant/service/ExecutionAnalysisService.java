package com.personalassistant.service;

import com.personalassistant.ai.AiService;
import com.personalassistant.dto.ExecutionAnalysisResponse;
import com.personalassistant.entity.ExecutionAnalysis;
import com.personalassistant.entity.Task;
import com.personalassistant.entity.TaskExecution;
import com.personalassistant.entity.TaskExecutionStatus;
import com.personalassistant.exception.ConflictException;
import com.personalassistant.exception.TaskNotFoundException;
import com.personalassistant.mapper.ExecutionAnalysisMapper;
import com.personalassistant.repository.ExecutionAnalysisRepository;
import com.personalassistant.repository.TaskExecutionRepository;
import com.personalassistant.repository.TaskRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.Instant;
import java.util.UUID;

@Service
@Transactional
public class ExecutionAnalysisService {

    private static final long MAX_EXECUTION_MINUTES =
            8 * 60;

    private final ExecutionAnalysisRepository
            executionAnalysisRepository;

    private final TaskExecutionRepository
            taskExecutionRepository;

    private final TaskRepository taskRepository;

    private final AiService aiService;

    private final ExecutionAnalysisMapper
            executionAnalysisMapper;

    public ExecutionAnalysisService(
            ExecutionAnalysisRepository executionAnalysisRepository,
            TaskExecutionRepository taskExecutionRepository,
            TaskRepository taskRepository,
            AiService aiService,
            ExecutionAnalysisMapper executionAnalysisMapper
    ) {
        this.executionAnalysisRepository =
                executionAnalysisRepository;

        this.taskExecutionRepository =
                taskExecutionRepository;

        this.taskRepository =
                taskRepository;

        this.aiService =
                aiService;

        this.executionAnalysisMapper =
                executionAnalysisMapper;
    }

    /**
     * Analyzes a completed task execution.
     *
     * Business rules:
     *
     * - task must belong to authenticated user
     * - execution must belong to task + user
     * - execution must be COMPLETED
     * - start/end timestamps must exist
     * - end must be after start
     * - execution duration must be reasonable
     * - an execution is analyzed only once
     */
    public ExecutionAnalysisResponse analyzeExecution(
            UUID taskId,
            UUID executionId,
            UUID userId
    ) {

        Task task =
                taskRepository
                        .findByIdAndUserId(
                                taskId,
                                userId
                        )
                        .orElseThrow(() ->
                                new TaskNotFoundException(
                                        "Task not found."
                                )
                        );

        TaskExecution execution =
                taskExecutionRepository
                        .findByIdAndTaskIdAndUserId(
                                executionId,
                                taskId,
                                userId
                        )
                        .orElseThrow(() ->
                                new TaskNotFoundException(
                                        "Task execution not found."
                                )
                        );

        validateExecutionCanBeAnalyzed(
                execution
        );

        /*
         * Do not call AI again when an analysis
         * already exists.
         */
        ExecutionAnalysis existingAnalysis =
                executionAnalysisRepository
                        .findByExecutionId(
                                executionId
                        )
                        .orElse(null);

        if (existingAnalysis != null) {
            return executionAnalysisMapper
                    .toResponse(existingAnalysis);
        }

        /*
         * Duration is calculated deterministically
         * by the backend.
         */
        long actualMinutes =
                calculateActualMinutes(
                        execution
                );

        /*
         * AI interprets the execution.
         *
         * AI does NOT determine the authoritative
         * execution duration.
         */
        com.personalassistant.dto.ExecutionAnalysis
                aiAnalysis =
                aiService.analyzeExecution(
                        task.getTitle(),
                        task.getDescription(),
                        task.getEstimatedMinutes(),
                        actualMinutes,
                        execution.getFeedback()
                );

        if (aiAnalysis == null) {
            throw new ConflictException(
                    "AI returned an empty execution analysis."
            );
        }

        /*
         * Convert AI result to our persistence model.
         */
        ExecutionAnalysis entity =
                new ExecutionAnalysis();

        entity.setExecution(
                execution
        );

        entity.setDifficulty(
                aiAnalysis.difficulty()
        );

        entity.setTimeAssessment(
                aiAnalysis.timeAssessment()
        );

        entity.setBlocker(
                aiAnalysis.blocker()
        );

        entity.setInsight(
                aiAnalysis.insight()
        );

        entity.setSuggestion(
                aiAnalysis.suggestion()
        );

        ExecutionAnalysis savedAnalysis =
                executionAnalysisRepository.save(
                        entity
                );

        return executionAnalysisMapper
                .toResponse(savedAnalysis);
    }

    /**
     * Returns the analysis belonging to the
     * authenticated user's execution.
     */
    @Transactional(readOnly = true)
    public ExecutionAnalysisResponse getExecutionAnalysis(
            UUID taskId,
            UUID executionId,
            UUID userId
    ) {

        taskRepository
                .findByIdAndUserId(
                        taskId,
                        userId
                )
                .orElseThrow(() ->
                        new TaskNotFoundException(
                                "Task not found."
                        )
                );

        taskExecutionRepository
                .findByIdAndTaskIdAndUserId(
                        executionId,
                        taskId,
                        userId
                )
                .orElseThrow(() ->
                        new TaskNotFoundException(
                                "Task execution not found."
                        )
                );

        return executionAnalysisRepository
                .findByExecutionId(
                        executionId
                )
                .map(
                        executionAnalysisMapper::toResponse
                )
                .orElseThrow(() ->
                        new TaskNotFoundException(
                                "Execution analysis not found."
                        )
                );
    }

    /**
     * Validates the execution lifecycle state before
     * sending anything to AI.
     */
    private void validateExecutionCanBeAnalyzed(
            TaskExecution execution
    ) {

        if (execution.getStatus()
                != TaskExecutionStatus.COMPLETED) {

            throw new ConflictException(
                    "Execution must be completed before analysis."
            );
        }

        if (execution.getStartedAt() == null) {
            throw new ConflictException(
                    "Execution has no start time."
            );
        }

        if (execution.getEndedAt() == null) {
            throw new ConflictException(
                    "Execution has no end time."
            );
        }

        if (!execution.getEndedAt()
                .isAfter(execution.getStartedAt())) {

            throw new ConflictException(
                    "Execution end time must be after start time."
            );
        }
    }

    /**
     * Calculates actual execution duration.
     */
    private long calculateActualMinutes(
            TaskExecution execution
    ) {

        Instant startedAt =
                execution.getStartedAt();

        Instant endedAt =
                execution.getEndedAt();

        long actualMinutes =
                Duration
                        .between(
                                startedAt,
                                endedAt
                        )
                        .toMinutes();

        if (actualMinutes <= 0) {
            throw new ConflictException(
                    "Execution duration must be greater than zero."
            );
        }

        if (actualMinutes >
                MAX_EXECUTION_MINUTES) {

            throw new ConflictException(
                    "Execution duration exceeds the maximum supported duration."
            );
        }

        return actualMinutes;
    }
}