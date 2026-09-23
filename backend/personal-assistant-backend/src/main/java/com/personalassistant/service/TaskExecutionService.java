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

    public TaskExecutionService(
            TaskExecutionRepository taskExecutionRepository,
            TaskRepository taskRepository,
            UserRepository userRepository,
            TaskExecutionMapper taskExecutionMapper
    ) {
        this.taskExecutionRepository = taskExecutionRepository;
        this.taskRepository = taskRepository;
        this.userRepository = userRepository;
        this.taskExecutionMapper = taskExecutionMapper;
    }

    public TaskExecutionResponse startExecution(
            UUID taskId,
            UUID userId,
            CreateTaskExecutionRequest request
    ) {

        Task task = getOwnedTask(taskId, userId);

        User user = userRepository.findById(userId)
                .orElseThrow(() ->
                        new UserNotFoundException("User not found")
                );

        boolean activeExecutionExists =
                taskExecutionRepository
                        .existsByTaskIdAndUserIdAndStatus(
                                taskId,
                                userId,
                                TaskExecutionStatus.STARTED
                        );

        if (activeExecutionExists) {
            throw new ConflictException(
                    "Task already has an active execution"
            );
        }

        TaskExecution execution = new TaskExecution();

        execution.setTask(task);
        execution.setUser(user);
        execution.setStartedAt(Instant.now());
        execution.setStatus(TaskExecutionStatus.STARTED);

        if (request != null) {
            execution.setFeedback(request.feedback());
        }

        TaskExecution savedExecution =
                taskExecutionRepository.save(execution);

        return taskExecutionMapper.toResponse(savedExecution);
    }

    public TaskExecutionResponse updateExecution(
            UUID taskId,
            UUID executionId,
            UUID userId,
            UpdateTaskExecutionRequest request
    ) {

        getOwnedTask(taskId, userId);

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

        if (request.status() != null) {

            execution.setStatus(request.status());

            if (request.status() == TaskExecutionStatus.COMPLETED
                    || request.status() == TaskExecutionStatus.CANCELLED) {

                execution.setEndedAt(Instant.now());
            }
        }

        if (request.feedback() != null) {
            execution.setFeedback(request.feedback());
        }

        return taskExecutionMapper.toResponse(execution);
    }

    @Transactional(readOnly = true)
    public List<TaskExecutionResponse> getExecutions(
            UUID taskId,
            UUID userId
    ) {

        getOwnedTask(taskId, userId);

        return taskExecutionRepository
                .findByTaskIdAndUserIdOrderByStartedAtDesc(
                        taskId,
                        userId
                )
                .stream()
                .map(taskExecutionMapper::toResponse)
                .toList();
    }

    private Task getOwnedTask(
            UUID taskId,
            UUID userId
    ) {

        return taskRepository
                .findByIdAndUserId(taskId, userId)
                .orElseThrow(() ->
                        new TaskNotFoundException(
                                "Task not found"
                        )
                );
    }
}