package com.personalassistant.dto;

import com.personalassistant.entity.AiProcessingStatus;
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
        AiProcessingStatus aiStatus,
        String aiError,
        String aiSummary,
        Instant createdAt,
        Instant updatedAt
) {}