package com.personalassistant.controller;

import com.personalassistant.dto.ExecutionAnalysisResponse;
import com.personalassistant.service.ExecutionAnalysisService;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/tasks/{taskId}/executions/{executionId}/analysis")
public class ExecutionAnalysisController {

    private final ExecutionAnalysisService executionAnalysisService;

    public ExecutionAnalysisController(
            ExecutionAnalysisService executionAnalysisService
    ) {
        this.executionAnalysisService = executionAnalysisService;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public ExecutionAnalysisResponse analyzeExecution(
            @PathVariable UUID taskId,
            @PathVariable UUID executionId,
            Authentication authentication
    ) {

        UUID userId =
                UUID.fromString(authentication.getName());

        return executionAnalysisService.analyzeExecution(
                taskId,
                executionId,
                userId
        );
    }

    @GetMapping
    public ExecutionAnalysisResponse getExecutionAnalysis(
            @PathVariable UUID taskId,
            @PathVariable UUID executionId,
            Authentication authentication
    ) {

        UUID userId =
                UUID.fromString(authentication.getName());

        return executionAnalysisService.getExecutionAnalysis(
                taskId,
                executionId,
                userId
        );
    }
}