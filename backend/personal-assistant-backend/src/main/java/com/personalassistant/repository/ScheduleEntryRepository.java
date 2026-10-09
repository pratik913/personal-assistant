package com.personalassistant.repository;

import com.personalassistant.entity.ScheduleEntry;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface ScheduleEntryRepository
        extends JpaRepository<ScheduleEntry, UUID> {

    List<ScheduleEntry> findByUserId(
            UUID userId
    );

    Optional<ScheduleEntry> findByIdAndUserId(
            UUID scheduleEntryId,
            UUID userId
    );

    List<ScheduleEntry> findByTaskIdAndUserId(
            UUID taskId,
            UUID userId
    );

    /**
     * Finds schedule entries that overlap the supplied
     * time range for the authenticated user.
     *
     * Overlap rule:
     *
     * existing.start < requested.end
     * AND
     * existing.end > requested.start
     *
     * The boundaries are therefore treated as non-overlapping:
     *
     * 09:00 - 10:00
     * 10:00 - 11:00
     *
     * are valid.
     */
    @Query("""
            SELECT s
            FROM ScheduleEntry s
            WHERE s.user.id = :userId
              AND s.startAt < :endAt
              AND s.endAt > :startAt
            """)
    List<ScheduleEntry> findOverlappingEntries(
            @Param("userId") UUID userId,
            @Param("startAt") Instant startAt,
            @Param("endAt") Instant endAt
    );

    /**
     * Same overlap query used while updating an existing
     * schedule entry, excluding that entry itself.
     */
    @Query("""
            SELECT s
            FROM ScheduleEntry s
            WHERE s.user.id = :userId
              AND s.id <> :scheduleEntryId
              AND s.startAt < :endAt
              AND s.endAt > :startAt
            """)
    List<ScheduleEntry> findOverlappingEntriesExcludingId(
            @Param("userId") UUID userId,
            @Param("scheduleEntryId") UUID scheduleEntryId,
            @Param("startAt") Instant startAt,
            @Param("endAt") Instant endAt
    );
}