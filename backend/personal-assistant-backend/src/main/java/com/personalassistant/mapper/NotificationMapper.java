package com.personalassistant.mapper;

import com.personalassistant.dto.NotificationResponse;
import com.personalassistant.entity.Notification;
import org.springframework.stereotype.Component;

@Component
public class NotificationMapper {

    public NotificationResponse toResponse(
            Notification notification
    ) {

        return new NotificationResponse(
                notification.getId(),
                notification.getTask().getId(),
                notification.getTask().getTitle(),
                notification.getType(),
                notification.getStatus(),
                notification.getTitle(),
                notification.getMessage(),
                notification.getScheduledAt(),
                notification.getReadAt(),
                notification.getCreatedAt()
        );

    }

}