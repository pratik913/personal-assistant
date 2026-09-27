package com.personalassistant.controller;

import com.personalassistant.dto.CaptureResponse;
import com.personalassistant.dto.CreateCaptureRequest;
import com.personalassistant.service.CaptureService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/captures")
public class CaptureController {

    private final CaptureService captureService;

    public CaptureController(
            CaptureService captureService
    ) {

        this.captureService =
                captureService;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public CaptureResponse createCapture(
            @Valid
            @RequestBody
            CreateCaptureRequest request,

            Authentication authentication
    ) {

        UUID userId =
                UUID.fromString(
                        authentication.getName()
                );

        return captureService.createCapture(
                request,
                userId
        );
    }

    @PostMapping(
            value = "/image",
            consumes = MediaType.MULTIPART_FORM_DATA_VALUE
    )
    @ResponseStatus(HttpStatus.CREATED)
    public CaptureResponse createImageCapture(
            @RequestPart("file")
            MultipartFile file,

            @RequestPart(
                    value = "content",
                    required = false
            )
            String content,

            Authentication authentication
    ) {

        UUID userId =
                UUID.fromString(
                        authentication.getName()
                );

        return captureService.createImageCapture(
                file,
                content,
                userId
        );
    }

    @PostMapping(
            value = "/voice",
            consumes = MediaType.MULTIPART_FORM_DATA_VALUE
    )
    @ResponseStatus(HttpStatus.CREATED)
    public CaptureResponse createVoiceCapture(
            @RequestPart("file")
            MultipartFile file,

            @RequestPart(
                    value = "content",
                    required = false
            )
            String content,

            Authentication authentication
    ) {

        UUID userId =
                UUID.fromString(
                        authentication.getName()
                );

        return captureService.createVoiceCapture(
                file,
                content,
                userId
        );
    }

    @GetMapping
    public List<CaptureResponse> getMyCaptures(
            Authentication authentication
    ) {

        UUID userId =
                UUID.fromString(
                        authentication.getName()
                );

        return captureService.getMyCaptures(
                userId
        );
    }

    @GetMapping("/{captureId}")
    public CaptureResponse getCapture(
            @PathVariable UUID captureId,

            Authentication authentication
    ) {

        UUID userId =
                UUID.fromString(
                        authentication.getName()
                );

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

        UUID userId =
                UUID.fromString(
                        authentication.getName()
                );

        captureService.deleteCapture(
                captureId,
                userId
        );
    }

    @PostMapping("/{captureId}/retry-ai")
    public ResponseEntity<CaptureResponse> retryAi(
            @PathVariable UUID captureId,

            Authentication authentication
    ) {

        UUID userId =
                UUID.fromString(
                        authentication.getName()
                );

        CaptureResponse response =
                captureService.retryAiProcessing(
                        captureId,
                        userId
                );

        return ResponseEntity.ok(
                response
        );
    }
}