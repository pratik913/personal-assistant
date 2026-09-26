package com.personalassistant.repository;

import com.personalassistant.entity.TaskExecution;
import com.personalassistant.entity.TaskExecutionStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface TaskExecutionRepository
        extends JpaRepository<TaskExecution, UUID> {

    List<TaskExecution> findByTaskIdAndUserIdOrderByStartedAtDesc(
            UUID taskId,
            UUID userId
    );

    List<TaskExecution> findByTaskIdAndUserIdAndStatusOrderByStartedAtDesc(
            UUID taskId,
            UUID userId,
            TaskExecutionStatus status
    );

    Optional<TaskExecution> findByIdAndTaskIdAndUserId(
            UUID executionId,
            UUID taskId,
            UUID userId
    );

    boolean existsByTaskIdAndUserIdAndStatus(
            UUID taskId,
            UUID userId,
            TaskExecutionStatus status
    );
}