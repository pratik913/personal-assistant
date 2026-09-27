package com.personalassistant.controller;

import com.personalassistant.dto.UpdateUserPreferenceRequest;
import com.personalassistant.dto.UserPreferenceResponse;
import com.personalassistant.service.UserPreferenceService;
import jakarta.validation.Valid;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/users/me/preferences")
public class UserPreferenceController {

    private final UserPreferenceService userPreferenceService;

    public UserPreferenceController(
            UserPreferenceService userPreferenceService
    ) {

        this.userPreferenceService =
                userPreferenceService;
    }

    @GetMapping
    public UserPreferenceResponse getPreferences(
            Authentication authentication
    ) {

        return userPreferenceService.getPreferences(
                getUserId(authentication)
        );
    }

    @PatchMapping
    public UserPreferenceResponse updatePreferences(
            Authentication authentication,
            @Valid @RequestBody
            UpdateUserPreferenceRequest request
    ) {

        return userPreferenceService.updatePreferences(
                getUserId(authentication),
                request
        );
    }

    private UUID getUserId(
            Authentication authentication
    ) {

        return UUID.fromString(
                authentication.getName()
        );
    }
}