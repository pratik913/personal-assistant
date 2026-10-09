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
         * NOTIFICATION CORRELATION
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

        try {

            notificationCorrelationService
                    .findNotificationForExecution(
                            userId,
                            taskId,
                            startedAt
                    )
                    .ifPresent(
                            execution::setNotification
                    );

        } catch (RuntimeException ignored) {

            /*
             * Notification correlation is intelligence,
             * not a hard dependency for task execution.
             *
             * If correlation fails, execution should
             * continue normally.
             */

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


        if (request == null) {

            throw new IllegalArgumentException(
                    "Execution update request cannot be null."
            );

        }


        /*
         * =================================================
         * STATUS TRANSITION VALIDATION
         * =================================================
         */

        if (
                request.status() != null
        ) {

            validateStatusTransition(
                    execution.getStatus(),
                    request.status()
            );


            execution.setStatus(
                    request.status()
            );

        }


        /*
         * =================================================
         * FEEDBACK
         * =================================================
         */

        if (
                request.feedback() != null
        ) {

            execution.setFeedback(
                    request.feedback()
            );

        }


        /*
         * =================================================
         * COMPLETION / CANCELLATION
         * =================================================
         */

        if (
                request.status() ==
                        TaskExecutionStatus.COMPLETED ||
                        request.status() ==
                                TaskExecutionStatus.CANCELLED
        ) {

            Instant endedAt =
                    execution.getEndedAt() != null
                            ? execution.getEndedAt()
                            : Instant.now();


            validateExecutionTimes(
                    execution.getStartedAt(),
                    endedAt
            );


            execution.setEndedAt(
                    endedAt
            );

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
                execution == null
        ) {

            throw new IllegalArgumentException(
                    "Execution cannot be null."
            );

        }


        if (
                execution.getStartedAt() == null ||
                        execution.getEndedAt() == null
        ) {

            throw new IllegalStateException(
                    "Execution has not been completed."
            );

        }


        validateExecutionTimes(
                execution.getStartedAt(),
                execution.getEndedAt()
        );


        return Duration
                .between(
                        execution.getStartedAt(),
                        execution.getEndedAt()
                )
                .toMinutes();

    }


    /*
     * =====================================================
     * STATUS TRANSITION VALIDATION
     * =====================================================
     *
     * Valid lifecycle:
     *
     * STARTED
     *    ↓
     * COMPLETED
     *
     * STARTED
     *    ↓
     * CANCELLED
     *
     * Terminal states cannot be changed again.
     */

    private void validateStatusTransition(
            TaskExecutionStatus currentStatus,
            TaskExecutionStatus requestedStatus
    ) {

        if (
                currentStatus == null
        ) {

            throw new IllegalStateException(
                    "Current execution status is missing."
            );

        }


        if (
                requestedStatus == null
        ) {

            throw new IllegalArgumentException(
                    "Execution status is required."
            );

        }


        /*
         * Same status is allowed for idempotent updates.
         */

        if (
                currentStatus == requestedStatus
        ) {

            return;

        }


        /*
         * STARTED can transition only to
         * COMPLETED or CANCELLED.
         */

        if (
                currentStatus ==
                        TaskExecutionStatus.STARTED
        ) {

            if (
                    requestedStatus ==
                            TaskExecutionStatus.COMPLETED ||
                            requestedStatus ==
                                    TaskExecutionStatus.CANCELLED
            ) {

                return;

            }

        }


        /*
         * COMPLETED and CANCELLED are terminal.
         */

        throw new IllegalStateException(
                "Invalid execution status transition: "
                        + currentStatus
                        + " -> "
                        + requestedStatus
        );

    }


    /*
     * =====================================================
     * EXECUTION TIME VALIDATION
     * =====================================================
     */

    private void validateExecutionTimes(
            Instant startedAt,
            Instant endedAt
    ) {

        if (
                startedAt == null ||
                        endedAt == null
        ) {

            throw new IllegalArgumentException(
                    "Execution start and end times are required."
            );

        }


        if (
                endedAt.isBefore(
                        startedAt
                )
        ) {

            throw new IllegalArgumentException(
                    "Execution end time cannot be before start time."
            );

        }


        long minutes =
                Duration
                        .between(
                                startedAt,
                                endedAt
                        )
                        .toMinutes();


        /*
         * Prevent corrupted execution history from
         * influencing adaptive planning.
         */

        if (
                minutes > 8 * 60
        ) {

            throw new IllegalArgumentException(
                    "Execution duration cannot exceed 8 hours."
            );

        }

    }

}