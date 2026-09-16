package com.personalassistant.service;

import com.personalassistant.dto.AiCaptureAnalysis;
import com.personalassistant.dto.CreateTaskRequest;
import com.personalassistant.dto.TaskResponse;
import com.personalassistant.dto.UpdateTaskRequest;
import com.personalassistant.entity.*;
import com.personalassistant.exception.CaptureNotFoundException;
import com.personalassistant.exception.TaskNotFoundException;
import com.personalassistant.exception.UserNotFoundException;
import com.personalassistant.mapper.TaskMapper;
import com.personalassistant.repository.CaptureRepository;
import com.personalassistant.repository.TaskRepository;
import com.personalassistant.repository.UserRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
@Transactional
public class TaskService {

    private final TaskRepository taskRepository;
    private final UserRepository userRepository;
    private final TaskMapper taskMapper;
    private final CaptureRepository captureRepository;

    public TaskService(
            TaskRepository taskRepository,
            UserRepository userRepository,
            TaskMapper taskMapper,
            CaptureRepository captureRepository
    ) {
        this.taskRepository = taskRepository;
        this.userRepository = userRepository;
        this.taskMapper = taskMapper;
        this.captureRepository = captureRepository;
    }

    public TaskResponse createTask(
            CreateTaskRequest request,
            UUID userId
    ) {

        User user = userRepository.findById(userId)
                .orElseThrow(() ->
                        new UserNotFoundException("User not found")
                );

        Task task = taskMapper.toEntity(request);

        task.setUser(user);

        if (request.getCaptureId() != null) {

            Capture capture = captureRepository
                    .findByIdAndUserId(
                            request.getCaptureId(),
                            userId
                    )
                    .orElseThrow(() ->
                            new CaptureNotFoundException("Capture not found")
                    );

            task.setCapture(capture);
        }

        Task savedTask = taskRepository.save(task);

        return taskMapper.toResponse(savedTask);
    }

    @Transactional(readOnly = true)
    public List<TaskResponse> getMyTasks(UUID userId) {

        return taskRepository.findByUserId(userId)
                .stream()
                .map(taskMapper::toResponse)
                .toList();
    }


    public void deleteTask(
            UUID taskId,
            UUID userId
    ) {

        Task task = taskRepository
                .findByIdAndUserId(taskId, userId)
                .orElseThrow(() ->
                        new TaskNotFoundException("Task not found")
                );

        taskRepository.delete(task);
    }

    @Transactional(readOnly = true)
    public TaskResponse getTask(
            UUID taskId,
            UUID userId
    ) {

        Task task = taskRepository
                .findByIdAndUserId(taskId, userId)
                .orElseThrow(() ->
                        new TaskNotFoundException("Task not found")
                );

        return taskMapper.toResponse(task);
    }

    public TaskResponse updateTask(
            UUID taskId,
            UUID userId,
            UpdateTaskRequest request
    ) {

        Task task = taskRepository
                .findByIdAndUserId(taskId, userId)
                .orElseThrow(() ->
                        new TaskNotFoundException("Task not found")
                );

        if (request.getTitle() != null) {
            task.setTitle(request.getTitle());
        }

        if (request.getDescription() != null) {
            task.setDescription(request.getDescription());
        }

        if (request.getStatus() != null) {
            task.setStatus(request.getStatus());
        }

        if (request.getPriority() != null) {
            task.setPriority(request.getPriority());
        }

        if (request.getDueDate() != null) {
            task.setDueDate(request.getDueDate());
        }

        if (request.getEstimatedMinutes() != null) {
            task.setEstimatedMinutes(request.getEstimatedMinutes());
        }

        Task updatedTask = taskRepository.save(task);

        return taskMapper.toResponse(updatedTask);
    }

    @Transactional(readOnly = true)
    public List<TaskResponse> getTasksByCapture(
            UUID captureId,
            UUID userId
    ) {

        Capture capture = captureRepository
                .findByIdAndUserId(captureId, userId)
                .orElseThrow(() ->
                        new CaptureNotFoundException("Capture not found")
                );

        return taskRepository
                .findByCaptureIdAndUserId(captureId, userId)
                .stream()
                .map(taskMapper::toResponse)
                .toList();
    }

    public List<TaskResponse> createTasksFromAiAnalysis(
            AiCaptureAnalysis analysis,
            UUID userId,
            UUID captureId
    ) {

        User user = userRepository.findById(userId)
                .orElseThrow(() ->
                        new UserNotFoundException("User not found")
                );

        Capture capture = captureRepository
                .findByIdAndUserId(captureId, userId)
                .orElseThrow(() ->
                        new CaptureNotFoundException("Capture not found")
                );

        return analysis.tasks()
                .stream()
                .map(suggestion -> {

                    Task task = new Task();

                    task.setTitle(suggestion.title());
                    task.setDescription(suggestion.description());
                    task.setEstimatedMinutes(
                            suggestion.estimatedMinutes()
                    );

                    task.setStatus(TaskStatus.TODO);
                    task.setPriority(TaskPriority.MEDIUM);

                    task.setUser(user);
                    task.setCapture(capture);

                    return taskRepository.save(task);
                })
                .map(taskMapper::toResponse)
                .toList();
    }
}