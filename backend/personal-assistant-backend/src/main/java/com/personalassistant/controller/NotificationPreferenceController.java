package com.personalassistant.controller;

import com.personalassistant.dto.NotificationPreferenceResponse;
import com.personalassistant.dto.UpdateNotificationPreferenceRequest;
import com.personalassistant.service.NotificationPreferenceService;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

import org.springframework.security.core.Authentication;

@RestController
@RequestMapping("/api/notification-preferences")
public class NotificationPreferenceController {

    private final NotificationPreferenceService
            notificationPreferenceService;

    public NotificationPreferenceController(
            NotificationPreferenceService notificationPreferenceService
    ) {

        this.notificationPreferenceService =
                notificationPreferenceService;
    }

    @GetMapping
    public NotificationPreferenceResponse getPreferences(
            Authentication authentication
    ) {

        UUID userId =
                UUID.fromString(
                        authentication.getName()
                );

        return notificationPreferenceService
                .getPreferences(userId);
    }

    @PatchMapping
    public NotificationPreferenceResponse updatePreferences(
            @Valid
            @RequestBody
            UpdateNotificationPreferenceRequest request,
            Authentication authentication
    ) {

        UUID userId =
                UUID.fromString(
                        authentication.getName()
                );

        return notificationPreferenceService
                .updatePreferences(
                        userId,
                        request
                );
    }
}