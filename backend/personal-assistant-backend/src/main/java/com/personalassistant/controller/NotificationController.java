package com.personalassistant.controller;

import com.personalassistant.dto.NotificationResponse;
import com.personalassistant.dto.UnreadNotificationCountResponse;
import com.personalassistant.service.NotificationService;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/notifications")
public class NotificationController {

    private final NotificationService notificationService;


    public NotificationController(
            NotificationService notificationService
    ) {

        this.notificationService =
                notificationService;

    }


    /**
     * Returns all currently visible unread notifications.
     */
    @GetMapping("/unread")
    public List<NotificationResponse> getUnreadNotifications(
            Authentication authentication
    ) {

        UUID userId =
                UUID.fromString(
                        authentication.getName()
                );


        return notificationService
                .getUnreadNotifications(
                        userId
                );

    }


    /**
     * Returns the unread notification count.
     */
    @GetMapping("/unread/count")
    public UnreadNotificationCountResponse getUnreadCount(
            Authentication authentication
    ) {

        UUID userId =
                UUID.fromString(
                        authentication.getName()
                );


        return notificationService
                .getUnreadCount(
                        userId
                );

    }


    /**
     * Marks a notification as read.
     */
    @PatchMapping("/{notificationId}/read")
    @ResponseStatus(HttpStatus.OK)
    public NotificationResponse markAsRead(
            @PathVariable UUID notificationId,
            Authentication authentication
    ) {

        UUID userId =
                UUID.fromString(
                        authentication.getName()
                );


        return notificationService
                .markAsRead(
                        userId,
                        notificationId
                );

    }

}