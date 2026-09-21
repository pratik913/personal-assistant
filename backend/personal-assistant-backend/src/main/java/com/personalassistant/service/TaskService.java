package com.personalassistant.service;

import com.personalassistant.dto.AiCaptureAnalysis;
import com.personalassistant.dto.CreateTaskRequest;
import com.personalassistant.dto.TaskResponse;
import com.personalassistant.dto.UpdateTaskRequest;
import com.personalassistant.entity.Capture;
import com.personalassistant.entity.Goal;
import com.personalassistant.entity.Task;
import com.personalassistant.entity.TaskStatus;
import com.personalassistant.entity.User;
import com.personalassistant.exception.CaptureNotFoundException;
import com.personalassistant.exception.GoalNotFoundException;
import com.personalassistant.exception.TaskNotFoundException;
import com.personalassistant.exception.UserNotFoundException;
import com.personalassistant.mapper.TaskMapper;
import com.personalassistant.repository.CaptureRepository;
import com.personalassistant.repository.GoalRepository;
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
    private final GoalRepository goalRepository;

    public TaskService(
            TaskRepository taskRepository,
            UserRepository userRepository,
            TaskMapper taskMapper,
            CaptureRepository captureRepository,
            GoalRepository goalRepository
    ) {
        this.taskRepository = taskRepository;
        this.userRepository = userRepository;
        this.taskMapper = taskMapper;
        this.captureRepository = captureRepository;
        this.goalRepository = goalRepository;
    }

    public TaskResponse createTask(
            CreateTaskRequest request,
            UUID userId
    ) {

        User user = userRepository.findById(userId)
                .orElseThrow(() ->
                        new UserNotFoundException(
                                "User not found"
                        )
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
                            new CaptureNotFoundException(
                                    "Capture not found"
                            )
                    );

            task.setCapture(capture);
        }

        if (request.getGoalId() != null) {

            Goal goal = goalRepository
                    .findByIdAndUserId(
                            request.getGoalId(),
                            userId
                    )
                    .orElseThrow(() ->
                            new GoalNotFoundException(
                                    "Goal not found"
                            )
                    );

            task.setGoal(goal);
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
                .findByIdAndUserId(
                        taskId,
                        userId
                )
                .orElseThrow(() ->
                        new TaskNotFoundException(
                                "Task not found"
                        )
                );

        taskRepository.delete(task);
    }

    @Transactional(readOnly = true)
    public TaskResponse getTask(
            UUID taskId,
            UUID userId
    ) {

        Task task = taskRepository
                .findByIdAndUserId(
                        taskId,
                        userId
                )
                .orElseThrow(() ->
                        new TaskNotFoundException(
                                "Task not found"
                        )
                );

        return taskMapper.toResponse(task);
    }

    public TaskResponse updateTask(
            UUID taskId,
            UUID userId,
            UpdateTaskRequest request
    ) {

        Task task = taskRepository
                .findByIdAndUserId(
                        taskId,
                        userId
                )
                .orElseThrow(() ->
                        new TaskNotFoundException(
                                "Task not found"
                        )
                );

        if (request.getTitle() != null) {
            task.setTitle(request.getTitle());
        }

        if (request.getDescription() != null) {
            task.setDescription(
                    request.getDescription()
            );
        }

        if (request.getStatus() != null) {
            task.setStatus(
                    request.getStatus()
            );
        }

        if (request.getPriority() != null) {
            task.setPriority(
                    request.getPriority()
            );
        }

        if (request.getDueDate() != null) {
            task.setDueDate(
                    request.getDueDate()
            );
        }

        if (request.getEstimatedMinutes() != null) {
            task.setEstimatedMinutes(
                    request.getEstimatedMinutes()
            );
        }

        /*
         * Assign task to a goal.
         *
         * The goal is searched using both:
         *   1. goalId
         *   2. authenticated userId
         *
         * This prevents a user from assigning
         * their task to another user's goal.
         */
        if (request.getGoalId() != null) {

            Goal goal = goalRepository
                    .findByIdAndUserId(
                            request.getGoalId(),
                            userId
                    )
                    .orElseThrow(() ->
                            new GoalNotFoundException(
                                    "Goal not found"
                            )
                    );

            task.setGoal(goal);
        }

        Task updatedTask =
                taskRepository.save(task);

        return taskMapper.toResponse(
                updatedTask
        );
    }

    /**
     * Removes the goal association from a task.
     *
     * The task is first looked up using both
     * taskId and authenticated userId so that
     * a user cannot modify another user's task.
     */
    public void removeTaskFromGoal(
            UUID taskId,
            UUID userId
    ) {

        Task task = taskRepository
                .findByIdAndUserId(
                        taskId,
                        userId
                )
                .orElseThrow(() ->
                        new TaskNotFoundException(
                                "Task not found"
                        )
                );

        task.setGoal(null);

        taskRepository.save(task);
    }

    @Transactional(readOnly = true)
    public List<TaskResponse> getTasksByCapture(
            UUID captureId,
            UUID userId
    ) {

        Capture capture = captureRepository
                .findByIdAndUserId(
                        captureId,
                        userId
                )
                .orElseThrow(() ->
                        new CaptureNotFoundException(
                                "Capture not found"
                        )
                );

        return taskRepository
                .findByCaptureIdAndUserId(
                        captureId,
                        userId
                )
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
                        new UserNotFoundException(
                                "User not found"
                        )
                );

        Capture capture = captureRepository
                .findByIdAndUserId(
                        captureId,
                        userId
                )
                .orElseThrow(() ->
                        new CaptureNotFoundException(
                                "Capture not found"
                        )
                );

        // Check whether AI tasks already exist
        List<Task> existingAiTasks =
                taskRepository
                        .findByCaptureIdAndUserIdAndAiGeneratedTrue(
                                captureId,
                                userId
                        );

        // Prevent duplicate AI-generated tasks
        if (!existingAiTasks.isEmpty()) {

            return existingAiTasks
                    .stream()
                    .map(taskMapper::toResponse)
                    .toList();
        }

        // Create new AI-generated tasks
        return analysis.tasks()
                .stream()
                .map(suggestion -> {

                    Task task = new Task();

                    task.setTitle(
                            suggestion.title()
                    );

                    task.setDescription(
                            suggestion.description()
                    );

                    task.setEstimatedMinutes(
                            suggestion.estimatedMinutes()
                    );

                    task.setStatus(
                            TaskStatus.TODO
                    );

                    // Use priority suggested by AI
                    task.setPriority(
                            suggestion.priority()
                    );

                    task.setUser(user);
                    task.setCapture(capture);

                    // AI-generated task
                    task.setAiGenerated(true);

                    return taskRepository.save(task);
                })
                .map(taskMapper::toResponse)
                .toList();
    }
}