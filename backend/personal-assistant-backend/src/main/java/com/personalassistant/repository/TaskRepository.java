package com.personalassistant.repository;

import com.personalassistant.entity.Task;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface TaskRepository extends JpaRepository<Task, UUID> {

    List<Task> findByUserId(UUID userId);

    Optional<Task> findByIdAndUserId(UUID taskId, UUID userId);

    List<Task> findByCaptureIdAndUserId(
            UUID captureId,
            UUID userId
    );
}