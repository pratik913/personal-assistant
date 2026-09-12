package com.personalassistant.service;

import com.personalassistant.dto.CreateTaskRequest;
import com.personalassistant.dto.TaskResponse;
import com.personalassistant.dto.UpdateTaskRequest;
import com.personalassistant.entity.Task;
import com.personalassistant.entity.User;
import com.personalassistant.exception.TaskNotFoundException;
import com.personalassistant.exception.UserNotFoundException;
import com.personalassistant.mapper.TaskMapper;
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

    public TaskService(
            TaskRepository taskRepository,
            UserRepository userRepository,
            TaskMapper taskMapper
    ) {
        this.taskRepository = taskRepository;
        this.userRepository = userRepository;
        this.taskMapper = taskMapper;
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
}