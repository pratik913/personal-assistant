package com.personalassistant.service;

import com.personalassistant.ai.AiService;
import com.personalassistant.dto.ExecutionAnalysisResponse;
import com.personalassistant.entity.Task;
import com.personalassistant.entity.TaskExecution;
import com.personalassistant.exception.ConflictException;
import com.personalassistant.exception.TaskNotFoundException;
import com.personalassistant.mapper.ExecutionAnalysisMapper;
import com.personalassistant.repository.ExecutionAnalysisRepository;
import com.personalassistant.repository.TaskExecutionRepository;
import com.personalassistant.repository.TaskRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.util.UUID;

@Service
@Transactional
public class ExecutionAnalysisService {

    private final ExecutionAnalysisRepository executionAnalysisRepository;
    private final TaskExecutionRepository taskExecutionRepository;
    private final TaskRepository taskRepository;
    private final AiService aiService;
    private final ExecutionAnalysisMapper executionAnalysisMapper;

    public ExecutionAnalysisService(
            ExecutionAnalysisRepository executionAnalysisRepository,
            TaskExecutionRepository taskExecutionRepository,
            TaskRepository taskRepository,
            AiService aiService,
            ExecutionAnalysisMapper executionAnalysisMapper
    ) {
        this.executionAnalysisRepository = executionAnalysisRepository;
        this.taskExecutionRepository = taskExecutionRepository;
        this.taskRepository = taskRepository;
        this.aiService = aiService;
        this.executionAnalysisMapper = executionAnalysisMapper;
    }

    public ExecutionAnalysisResponse analyzeExecution(
            UUID taskId,
            UUID executionId,
            UUID userId
    ) {

        Task task = taskRepository
                .findByIdAndUserId(taskId, userId)
                .orElseThrow(() ->
                        new TaskNotFoundException(
                                "Task not found"
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
                                        "Task execution not found"
                                )
                        );

        if (execution.getStatus()
                != com.personalassistant.entity.TaskExecutionStatus.COMPLETED) {

            throw new ConflictException(
                    "Execution must be completed before analysis"
            );
        }

        if (execution.getEndedAt() == null) {
            throw new ConflictException(
                    "Execution has no end time"
            );
        }

        /*
         * Do not call AI again if this execution
         * has already been analyzed.
         */
        var existingAnalysis =
                executionAnalysisRepository
                        .findByExecutionId(executionId)
                        .orElse(null);

        if (existingAnalysis != null) {
            return executionAnalysisMapper
                    .toResponse(existingAnalysis);
        }

        /*
         * Actual duration is calculated by the
         * application, not by AI.
         */
        long actualMinutes =
                Duration.between(
                        execution.getStartedAt(),
                        execution.getEndedAt()
                ).toMinutes();

        /*
         * Ask AI to interpret the execution.
         */
        com.personalassistant.dto.ExecutionAnalysis aiAnalysis =
                aiService.analyzeExecution(
                        task.getTitle(),
                        task.getDescription(),
                        task.getEstimatedMinutes(),
                        actualMinutes,
                        execution.getFeedback()
                );

        /*
         * Convert AI response into our database entity.
         */
        com.personalassistant.entity.ExecutionAnalysis entity =
                new com.personalassistant.entity.ExecutionAnalysis();

        entity.setExecution(execution);
        entity.setDifficulty(aiAnalysis.difficulty());
        entity.setTimeAssessment(
                aiAnalysis.timeAssessment()
        );
        entity.setBlocker(aiAnalysis.blocker());
        entity.setInsight(aiAnalysis.insight());
        entity.setSuggestion(aiAnalysis.suggestion());

        com.personalassistant.entity.ExecutionAnalysis savedAnalysis =
                executionAnalysisRepository.save(entity);

        return executionAnalysisMapper
                .toResponse(savedAnalysis);
    }

    @Transactional(readOnly = true)
    public ExecutionAnalysisResponse getExecutionAnalysis(
            UUID taskId,
            UUID executionId,
            UUID userId
    ) {

        taskRepository.findByIdAndUserId(
                taskId,
                userId
        ).orElseThrow(
                () -> new TaskNotFoundException("Task not found")
        );

        taskExecutionRepository.findByIdAndTaskIdAndUserId(
                executionId,
                taskId,
                userId
        ).orElseThrow(
                () -> new TaskNotFoundException("Task execution not found")
        );

        return executionAnalysisRepository
                .findByExecutionId(executionId)
                .map(executionAnalysisMapper::toResponse)
                .orElseThrow(
                        () -> new TaskNotFoundException(
                                "Execution analysis not found"
                        )
                );
    }


}