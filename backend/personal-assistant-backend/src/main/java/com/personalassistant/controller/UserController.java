package com.personalassistant.controller;

import com.personalassistant.dto.CreateUserRequest;
import com.personalassistant.dto.UserResponse;
import com.personalassistant.service.UserService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import org.springframework.security.core.Authentication;
@RestController
@RequestMapping("/api/users")
public class UserController {

    private final UserService userService;

    public UserController(UserService userService) {
        this.userService = userService;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public UserResponse createUser(
            @Valid @RequestBody CreateUserRequest request
    ) {
        return userService.createUser(request);
    }

    @GetMapping("/me")
    public UserResponse getCurrentUser(
            Authentication authentication
    ) {
        return userService.getCurrentUser(
                authentication.getName()
        );
    }
}