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

    boolean existsByUserIdAndTaskIdAndTypeAndScheduledAt(
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

    /*
     * Returns ALL unread notifications for a
     * specific task and notification type.
     *
     * Important:
     * No scheduledAt filter here.
     *
     * This allows NotificationService to detect
     * an old reminder even when the AI has changed
     * the task's scheduled time.
     */
    List<Notification>
    findByTaskIdAndUserIdAndTypeAndStatus(
            UUID taskId,
            UUID userId,
            NotificationType type,
            NotificationStatus status
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