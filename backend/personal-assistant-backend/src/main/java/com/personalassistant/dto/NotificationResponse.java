package com.personalassistant.dto;

import com.personalassistant.entity.NotificationStatus;
import com.personalassistant.entity.NotificationType;

import java.time.Instant;
import java.util.UUID;

public record NotificationResponse(

        UUID id,

        UUID taskId,

        String taskTitle,

        NotificationType type,

        NotificationStatus status,

        String title,

        String message,

        Instant scheduledAt,

        Instant readAt,

        Instant createdAt

) {
}