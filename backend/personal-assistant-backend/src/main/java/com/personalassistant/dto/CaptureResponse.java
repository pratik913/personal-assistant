package com.personalassistant.dto;

import com.personalassistant.entity.CaptureType;

import java.time.Instant;
import java.util.UUID;

public record CaptureResponse(
        UUID id,
        CaptureType type,
        String content,
        String sourceUrl,
        String storageUrl,
        String transcript,
        Instant createdAt,
        Instant updatedAt
) {
}