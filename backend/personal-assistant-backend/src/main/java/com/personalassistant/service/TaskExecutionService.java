package com.personalassistant.service;

import com.personalassistant.dto.CreateTaskExecutionRequest;
import com.personalassistant.dto.TaskExecutionResponse;
import com.personalassistant.dto.UpdateTaskExecutionRequest;
import com.personalassistant.entity.Task;
import com.personalassistant.entity.TaskExecution;
import com.personalassistant.entity.TaskExecutionStatus;
import com.personalassistant.entity.User;
import com.personalassistant.exception.ConflictException;
import com.personalassistant.exception.TaskNotFoundException;
import com.personalassistant.exception.UserNotFoundException;
import com.personalassistant.mapper.TaskExecutionMapper;
import com.personalassistant.repository.TaskExecutionRepository;
import com.personalassistant.repository.TaskRepository;
import com.personalassistant.repository.UserRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

@Service
@Transactional
public class TaskExecutionService {

    private final TaskExecutionRepository taskExecutionRepository;

    private final TaskRepository taskRepository;

    private final UserRepository userRepository;

    private final TaskExecutionMapper taskExecutionMapper;

    private final NotificationCorrelationService
            notificationCorrelationService;


    public TaskExecutionService(
            TaskExecutionRepository taskExecutionRepository,
            TaskRepository taskRepository,
            UserRepository userRepository,
            TaskExecutionMapper taskExecutionMapper,
            NotificationCorrelationService
                    notificationCorrelationService
    ) {

        this.taskExecutionRepository =
                taskExecutionRepository;

        this.taskRepository =
                taskRepository;

        this.userRepository =
                userRepository;

        this.taskExecutionMapper =
                taskExecutionMapper;

        this.notificationCorrelationService =
                notificationCorrelationService;

    }


    /*
     * =====================================================
     * START EXECUTION
     * =====================================================
     */

    public TaskExecutionResponse startExecution(
            UUID taskId,
            UUID userId,
            CreateTaskExecutionRequest request
    ) {

        User user =
                userRepository
                        .findById(userId)
                        .orElseThrow(() ->
                                new UserNotFoundException(
                                        "User not found."
                                )
                        );


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


        /*
         * Prevent multiple active executions
         * for the same task.
         */
        boolean alreadyRunning =
                taskExecutionRepository
                        .existsByTaskIdAndUserIdAndStatus(
                                taskId,
                                userId,
                                TaskExecutionStatus.STARTED
                        );


        if (alreadyRunning) {

            throw new ConflictException(
                    "This task already has an active execution."
            );

        }


        Instant startedAt =
                Instant.now();


        TaskExecution execution =
                new TaskExecution();


        execution.setTask(task);

        execution.setUser(user);

        execution.setStartedAt(
                startedAt
        );

        execution.setStatus(
                TaskExecutionStatus.STARTED
        );


        if (
                request != null &&
                        request.feedback() != null
        ) {

            execution.setFeedback(
                    request.feedback()
            );

        }


        /*
         * =================================================
         * DAY 26
         * =================================================
         *
         * Try to associate the execution with the
         * notification that preceded it.
         *
         * This is deliberately best-effort.
         *
         * Starting a task must NEVER fail simply because
         * notification correlation is unavailable.
         */

        notificationCorrelationService
                .findNotificationForExecution(
                        userId,
                        taskId,
                        startedAt
                )
                .ifPresent(
                        execution::setNotification
                );


        TaskExecution saved =
                taskExecutionRepository.save(
                        execution
                );


        return taskExecutionMapper.toResponse(
                saved
        );

    }


    /*
     * =====================================================
     * UPDATE EXECUTION
     * =====================================================
     */

    public TaskExecutionResponse updateExecution(
            UUID taskId,
            UUID executionId,
            UUID userId,
            UpdateTaskExecutionRequest request
    ) {

        TaskExecution execution =
                taskExecutionRepository
                        .findByIdAndTaskIdAndUserId(
                                executionId,
                                taskId,
                                userId
                        )
                        .orElseThrow(() ->
                                new IllegalStateException(
                                        "Task execution not found."
                                )
                        );


        if (
                request.status() != null
        ) {

            execution.setStatus(
                    request.status()
            );

        }


        if (
                request.feedback() != null
        ) {

            execution.setFeedback(
                    request.feedback()
            );

        }


        /*
         * Set endedAt when execution finishes.
         */
        if (
                request.status() ==
                        TaskExecutionStatus.COMPLETED ||
                        request.status() ==
                                TaskExecutionStatus.CANCELLED
        ) {

            if (
                    execution.getEndedAt() == null
            ) {

                execution.setEndedAt(
                        Instant.now()
                );

            }

        }


        TaskExecution saved =
                taskExecutionRepository.save(
                        execution
                );


        return taskExecutionMapper.toResponse(
                saved
        );

    }


    /*
     * =====================================================
     * HISTORY
     * =====================================================
     */

    @Transactional(readOnly = true)
    public List<TaskExecutionResponse> getExecutions(
            UUID taskId,
            UUID userId
    ) {

        /*
         * Ownership check.
         */
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


        return taskExecutionRepository
                .findByTaskIdAndUserIdOrderByStartedAtDesc(
                        taskId,
                        userId
                )
                .stream()
                .map(
                        taskExecutionMapper::toResponse
                )
                .toList();

    }


    /*
     * =====================================================
     * ACTUAL EXECUTION DURATION
     * =====================================================
     */

    public long calculateActualMinutes(
            TaskExecution execution
    ) {

        if (
                execution.getStartedAt() == null ||
                        execution.getEndedAt() == null
        ) {

            throw new IllegalStateException(
                    "Execution has not been completed."
            );

        }


        return Duration
                .between(
                        execution.getStartedAt(),
                        execution.getEndedAt()
                )
                .toMinutes();

    }

}