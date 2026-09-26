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

}