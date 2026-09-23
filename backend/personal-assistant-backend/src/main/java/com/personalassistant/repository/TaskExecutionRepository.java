package com.personalassistant.repository;

import com.personalassistant.entity.TaskExecution;
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

    Optional<TaskExecution> findByIdAndTaskIdAndUserId(
            UUID executionId,
            UUID taskId,
            UUID userId
    );

    boolean existsByTaskIdAndUserIdAndStatus(
            UUID taskId,
            UUID userId,
            com.personalassistant.entity.TaskExecutionStatus status
    );
}