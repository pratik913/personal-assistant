package com.personalassistant.controller;

import com.personalassistant.dto.CaptureResponse;
import com.personalassistant.dto.CreateCaptureRequest;
import com.personalassistant.service.CaptureService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/captures")
public class CaptureController {

    private final CaptureService captureService;

    public CaptureController(CaptureService captureService) {
        this.captureService = captureService;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public CaptureResponse createCapture(
            @Valid @RequestBody CreateCaptureRequest request,
            Authentication authentication
    ) {

        UUID userId = UUID.fromString(authentication.getName());

        return captureService.createCapture(
                request,
                userId
        );
    }

    @GetMapping
    public List<CaptureResponse> getMyCaptures(
            Authentication authentication
    ) {

        UUID userId = UUID.fromString(authentication.getName());

        return captureService.getMyCaptures(userId);
    }

    @GetMapping("/{captureId}")
    public CaptureResponse getCapture(
            @PathVariable UUID captureId,
            Authentication authentication
    ) {

        UUID userId = UUID.fromString(authentication.getName());

        return captureService.getCapture(
                captureId,
                userId
        );
    }

    @DeleteMapping("/{captureId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deleteCapture(
            @PathVariable UUID captureId,
            Authentication authentication
    ) {

        UUID userId = UUID.fromString(authentication.getName());

        captureService.deleteCapture(
                captureId,
                userId
        );
    }
}