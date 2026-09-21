package com.personalassistant.repository;

import com.personalassistant.entity.Task;
import com.personalassistant.entity.TaskStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface TaskRepository extends JpaRepository<Task, UUID> {

    List<Task> findByUserId(UUID userId);

    Optional<Task> findByIdAndUserId(
            UUID taskId,
            UUID userId
    );

    List<Task> findByCaptureIdAndUserId(
            UUID captureId,
            UUID userId
    );

    List<Task> findByCaptureIdAndUserIdAndAiGeneratedTrue(
            UUID captureId,
            UUID userId
    );

    List<Task> findByUserIdAndStatusNot(
            UUID userId,
            TaskStatus status
    );

    // Get all tasks belonging to a specific goal
    List<Task> findByGoalIdAndUserId(
            UUID goalId,
            UUID userId
    );

    // Count all tasks belonging to a specific goal
    long countByGoalIdAndUserId(
            UUID goalId,
            UUID userId
    );

    // Count completed tasks belonging to a specific goal
    long countByGoalIdAndUserIdAndStatus(
            UUID goalId,
            UUID userId,
            TaskStatus status
    );
}