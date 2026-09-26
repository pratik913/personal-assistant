package com.personalassistant.controller;

import com.personalassistant.dto.NotificationPreferenceResponse;
import com.personalassistant.dto.UpdateNotificationPreferenceRequest;
import com.personalassistant.service.NotificationPreferenceService;
import jakarta.validation.Valid;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

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

    /**
     * Returns the authenticated user's notification preferences.
     */
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

    /**
     * Partially updates the authenticated user's
     * notification preferences.
     */
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