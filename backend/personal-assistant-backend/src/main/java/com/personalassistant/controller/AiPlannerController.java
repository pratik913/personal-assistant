package com.personalassistant.controller;

import com.personalassistant.dto.AiPlanResponse;
import com.personalassistant.dto.CreateAiPlanRequest;
import com.personalassistant.service.AiPlannerService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/planner")
public class AiPlannerController {

    private final AiPlannerService aiPlannerService;

    public AiPlannerController(
            AiPlannerService aiPlannerService
    ) {
        this.aiPlannerService = aiPlannerService;
    }

    @PostMapping
    public ResponseEntity<AiPlanResponse> createPlan(
            @Valid @RequestBody CreateAiPlanRequest request,
            Authentication authentication
    ) {
        UUID userId = getAuthenticatedUserId(authentication);

        AiPlanResponse response =
                aiPlannerService.createPlan(
                        userId,
                        request
                );

        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(response);
    }

    private UUID getAuthenticatedUserId(
            Authentication authentication
    ) {
        return UUID.fromString(
                authentication.getName()
        );
    }
}