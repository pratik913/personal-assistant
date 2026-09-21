package com.personalassistant.repository;

import com.personalassistant.entity.Goal;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface GoalRepository extends JpaRepository<Goal, UUID> {

    List<Goal> findByUserId(UUID userId);

    Optional<Goal> findByIdAndUserId(UUID goalId, UUID userId);
}