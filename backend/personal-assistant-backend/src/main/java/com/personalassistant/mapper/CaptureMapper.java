package com.personalassistant.mapper;

import com.personalassistant.dto.CreateCaptureRequest;
import com.personalassistant.dto.CaptureResponse;
import com.personalassistant.entity.Capture;
import org.springframework.stereotype.Component;

@Component
public class CaptureMapper {

    public Capture toEntity(CreateCaptureRequest request) {

        Capture capture = new Capture();

        capture.setType(request.getType());
        capture.setContent(request.getContent());
        capture.setSourceUrl(request.getSourceUrl());

        return capture;
    }

    public CaptureResponse toResponse(Capture capture) {

        return new CaptureResponse(
                capture.getId(),
                capture.getType(),
                capture.getContent(),
                capture.getSourceUrl(),
                capture.getStorageUrl(),
                capture.getTranscript(),
                capture.getCreatedAt(),
                capture.getUpdatedAt()
        );
    }
}