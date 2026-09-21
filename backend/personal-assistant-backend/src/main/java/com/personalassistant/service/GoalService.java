package com.personalassistant.service;

import com.personalassistant.dto.CreateGoalRequest;
import com.personalassistant.dto.GoalResponse;
import com.personalassistant.dto.UpdateGoalRequest;
import com.personalassistant.dto.TaskResponse;
import com.personalassistant.entity.Goal;
import com.personalassistant.entity.Task;
import com.personalassistant.entity.TaskStatus;
import com.personalassistant.entity.User;
import com.personalassistant.exception.ResourceNotFoundException;
import com.personalassistant.mapper.GoalMapper;
import com.personalassistant.mapper.TaskMapper;
import com.personalassistant.repository.GoalRepository;
import com.personalassistant.repository.TaskRepository;
import com.personalassistant.repository.UserRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
public class GoalService {

    private final GoalRepository goalRepository;
    private final UserRepository userRepository;
    private final TaskRepository taskRepository;
    private final GoalMapper goalMapper;
    private final TaskMapper taskMapper;

    public GoalService(
            GoalRepository goalRepository,
            UserRepository userRepository,
            TaskRepository taskRepository,
            GoalMapper goalMapper,
            TaskMapper taskMapper
    ) {
        this.goalRepository = goalRepository;
        this.userRepository = userRepository;
        this.taskRepository = taskRepository;
        this.goalMapper = goalMapper;
        this.taskMapper = taskMapper;
    }

    @Transactional
    public GoalResponse createGoal(
            UUID userId,
            CreateGoalRequest request
    ) {

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new IllegalStateException(
                        "Authenticated user not found"
                ));

        Goal goal = new Goal();

        goal.setTitle(request.title());
        goal.setDescription(request.description());
        goal.setTargetDate(request.targetDate());
        goal.setUser(user);

        Goal savedGoal = goalRepository.save(goal);

        return goalMapper.toResponse(savedGoal);
    }

    @Transactional(readOnly = true)
    public List<GoalResponse> getGoals(UUID userId) {

        return goalRepository.findByUserId(userId)
                .stream()
                .map(goalMapper::toResponse)
                .toList();
    }

    @Transactional(readOnly = true)
    public GoalResponse getGoal(
            UUID userId,
            UUID goalId
    ) {

        Goal goal = getOwnedGoal(userId, goalId);

        return goalMapper.toResponse(goal);
    }

    @Transactional
    public GoalResponse updateGoal(
            UUID userId,
            UUID goalId,
            UpdateGoalRequest request
    ) {

        Goal goal = getOwnedGoal(userId, goalId);

        if (request.title() != null) {
            goal.setTitle(request.title());
        }

        if (request.description() != null) {
            goal.setDescription(request.description());
        }

        if (request.targetDate() != null) {
            goal.setTargetDate(request.targetDate());
        }

        Goal updatedGoal = goalRepository.save(goal);

        return goalMapper.toResponse(updatedGoal);
    }

    @Transactional
    public void deleteGoal(UUID userId, UUID goalId) {

        Goal goal = getOwnedGoal(userId, goalId);

        // Keep the tasks, but remove their association with this goal.
        List<Task> goalTasks = taskRepository.findByGoalIdAndUserId(
                goalId,
                userId
        );

        for (Task task : goalTasks) {
            task.setGoal(null);
        }

        if (!goalTasks.isEmpty()) {
            taskRepository.saveAll(goalTasks);
        }

        // Now the goal can safely be deleted.
        goalRepository.delete(goal);
    }
    @Transactional(readOnly = true)
    public List<TaskResponse> getTasksByGoal(
            UUID userId,
            UUID goalId
    ) {

        // First verify that the goal belongs to the authenticated user.
        getOwnedGoal(userId, goalId);

        return taskRepository
                .findByGoalIdAndUserId(
                        goalId,
                        userId
                )
                .stream()
                .map(taskMapper::toResponse)
                .toList();
    }

    /*
     * Get progress information for a specific goal.
     */
    @Transactional(readOnly = true)
    public GoalProgress getGoalProgress(
            UUID userId,
            UUID goalId
    ) {

        // Verify goal ownership first.
        getOwnedGoal(userId, goalId);

        long totalTasks =
                taskRepository.countByGoalIdAndUserId(
                        goalId,
                        userId
                );

        long completedTasks =
                taskRepository.countByGoalIdAndUserIdAndStatus(
                        goalId,
                        userId,
                        TaskStatus.COMPLETED
                );

        long remainingTasks =
                totalTasks - completedTasks;

        double progressPercentage = totalTasks == 0
                ? 0.0
                : (completedTasks * 100.0) / totalTasks;

        return new GoalProgress(
                totalTasks,
                completedTasks,
                remainingTasks,
                progressPercentage
        );
    }

    private Goal getOwnedGoal(
            UUID userId,
            UUID goalId
    ) {

        return goalRepository.findByIdAndUserId(
                        goalId,
                        userId
                )
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Goal not found"
                ));
    }

    /*
     * Response object for goal progress.
     */
    public record GoalProgress(
            long totalTasks,
            long completedTasks,
            long remainingTasks,
            double progressPercentage
    ) {
    }
}