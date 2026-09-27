package com.personalassistant.controller;

import com.personalassistant.dto.CreateUserRequest;
import com.personalassistant.dto.UserProfileResponse;
import com.personalassistant.dto.UserResponse;
import com.personalassistant.service.UserProfileService;
import com.personalassistant.service.UserService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/users")
public class UserController {

    private final UserService userService;
    private final UserProfileService userProfileService;

    public UserController(
            UserService userService,
            UserProfileService userProfileService
    ) {
        this.userService = userService;
        this.userProfileService = userProfileService;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public UserResponse createUser(
            @Valid @RequestBody CreateUserRequest request
    ) {
        return userService.createUser(request);
    }

    @GetMapping("/me")
    public UserProfileResponse getCurrentUser(
            Authentication authentication
    ) {
        UUID userId = UUID.fromString(
                authentication.getName()
        );

        return userProfileService.getProfile(userId);
    }
}