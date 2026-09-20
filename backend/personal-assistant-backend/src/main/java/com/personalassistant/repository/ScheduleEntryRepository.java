package com.personalassistant.repository;

import com.personalassistant.entity.ScheduleEntry;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface ScheduleEntryRepository
        extends JpaRepository<ScheduleEntry, UUID> {

    List<ScheduleEntry> findByUserId(UUID userId);

    Optional<ScheduleEntry> findByIdAndUserId(
            UUID scheduleEntryId,
            UUID userId
    );

    List<ScheduleEntry> findByTaskIdAndUserId(
            UUID taskId,
            UUID userId
    );
}