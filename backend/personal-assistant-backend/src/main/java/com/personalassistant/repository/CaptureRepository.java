package com.personalassistant.repository;

import com.personalassistant.entity.Capture;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface CaptureRepository extends JpaRepository<Capture, UUID> {

    List<Capture> findByUserId(UUID userId);

    Optional<Capture> findByIdAndUserId(UUID captureId, UUID userId);
}