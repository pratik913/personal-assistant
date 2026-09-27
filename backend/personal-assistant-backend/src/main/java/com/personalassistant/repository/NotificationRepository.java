package com.personalassistant.repository;

import com.personalassistant.entity.Notification;
import com.personalassistant.entity.NotificationStatus;
import com.personalassistant.entity.NotificationType;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface NotificationRepository
        extends JpaRepository<Notification, UUID> {


    List<Notification>
    findByUserIdAndStatusAndScheduledAtLessThanEqualOrderByScheduledAtDesc(
            UUID userId,
            NotificationStatus status,
            Instant scheduledAt
    );


    long countByUserIdAndStatusAndScheduledAtLessThanEqual(
            UUID userId,
            NotificationStatus status,
            Instant scheduledAt
    );


    Optional<Notification>
    findByIdAndUserId(
            UUID notificationId,
            UUID userId
    );


    boolean
    existsByUserIdAndTaskIdAndTypeAndScheduledAt(
            UUID userId,
            UUID taskId,
            NotificationType type,
            Instant scheduledAt
    );


    List<Notification>
    findByUserIdAndTypeAndStatusAndScheduledAtAfter(
            UUID userId,
            NotificationType type,
            NotificationStatus status,
            Instant scheduledAt
    );


    List<Notification>
    findByTaskIdAndUserIdAndTypeAndStatusAndScheduledAtAfter(
            UUID taskId,
            UUID userId,
            NotificationType type,
            NotificationStatus status,
            Instant scheduledAt
    );


    List<Notification>
    findByTaskIdAndUserIdAndTypeAndStatus(
            UUID taskId,
            UUID userId,
            NotificationType type,
            NotificationStatus status
    );


    List<Notification>
    findByUserIdAndTypeOrderByScheduledAtDesc(
            UUID userId,
            NotificationType type
    );


    /*
     * =====================================================
     * DAY 26
     * =====================================================
     *
     * Finds reminder history for a task, newest first,
     * up to the execution start time.
     *
     * NotificationCorrelationService additionally verifies:
     *
     * - notification was READ
     * - notification was read before execution
     * - notification is not already correlated
     * - response time is within the allowed window
     */

    List<Notification>
    findByTaskIdAndUserIdAndTypeAndStatusAndScheduledAtLessThanEqualOrderByScheduledAtDesc(
            UUID taskId,
            UUID userId,
            NotificationType type,
            NotificationStatus status,
            Instant scheduledAt
    );


    void deleteByTaskIdAndUserId(
            UUID taskId,
            UUID userId
    );


    void deleteByTaskIdAndUserIdAndStatusAndScheduledAtAfter(
            UUID taskId,
            UUID userId,
            NotificationStatus status,
            Instant scheduledAt
    );

}