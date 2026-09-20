package com.personalassistant.controller;

import com.personalassistant.dto.CreateScheduleEntryRequest;
import com.personalassistant.dto.ScheduleEntryResponse;
import com.personalassistant.dto.UpdateScheduleEntryRequest;
import com.personalassistant.service.ScheduleEntryService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/schedule")
public class ScheduleEntryController {

    private final ScheduleEntryService scheduleEntryService;

    public ScheduleEntryController(
            ScheduleEntryService scheduleEntryService
    ) {
        this.scheduleEntryService = scheduleEntryService;
    }

    @PostMapping
    public ResponseEntity<ScheduleEntryResponse> createScheduleEntry(
            @Valid @RequestBody CreateScheduleEntryRequest request,
            Authentication authentication
    ) {
        UUID userId = getAuthenticatedUserId(authentication);

        ScheduleEntryResponse response =
                scheduleEntryService.createScheduleEntry(
                        userId,
                        request
                );

        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(response);
    }

    @GetMapping
    public ResponseEntity<List<ScheduleEntryResponse>> getScheduleEntries(
            Authentication authentication
    ) {
        UUID userId = getAuthenticatedUserId(authentication);

        List<ScheduleEntryResponse> response =
                scheduleEntryService.getScheduleEntries(userId);

        return ResponseEntity.ok(response);
    }

    @GetMapping("/{id}")
    public ResponseEntity<ScheduleEntryResponse> getScheduleEntry(
            @PathVariable UUID id,
            Authentication authentication
    ) {
        UUID userId = getAuthenticatedUserId(authentication);

        ScheduleEntryResponse response =
                scheduleEntryService.getScheduleEntry(
                        id,
                        userId
                );

        return ResponseEntity.ok(response);
    }

    @PatchMapping("/{id}")
    public ResponseEntity<ScheduleEntryResponse> updateScheduleEntry(
            @PathVariable UUID id,
            @Valid @RequestBody UpdateScheduleEntryRequest request,
            Authentication authentication
    ) {
        UUID userId = getAuthenticatedUserId(authentication);

        ScheduleEntryResponse response =
                scheduleEntryService.updateScheduleEntry(
                        id,
                        userId,
                        request
                );

        return ResponseEntity.ok(response);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteScheduleEntry(
            @PathVariable UUID id,
            Authentication authentication
    ) {
        UUID userId = getAuthenticatedUserId(authentication);

        scheduleEntryService.deleteScheduleEntry(
                id,
                userId
        );

        return ResponseEntity.noContent().build();
    }

    private UUID getAuthenticatedUserId(
            Authentication authentication
    ) {
        return UUID.fromString(
                authentication.getName()
        );
    }
}