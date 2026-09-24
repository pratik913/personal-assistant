package com.personalassistant.repository;

import com.personalassistant.entity.ExecutionAnalysis;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface ExecutionAnalysisRepository
        extends JpaRepository<ExecutionAnalysis, UUID> {

    Optional<ExecutionAnalysis> findByExecutionId(UUID executionId);

    boolean existsByExecutionId(UUID executionId);
}