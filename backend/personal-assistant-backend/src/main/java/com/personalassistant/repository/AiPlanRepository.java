package com.personalassistant.repository;

import com.personalassistant.entity.AiPlan;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface AiPlanRepository
        extends JpaRepository<AiPlan, UUID> {

    List<AiPlan> findByUserId(UUID userId);

    Optional<AiPlan> findByIdAndUserId(
            UUID planId,
            UUID userId
    );
}