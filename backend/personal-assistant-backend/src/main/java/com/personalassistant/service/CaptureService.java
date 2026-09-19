package com.personalassistant.service;

import com.personalassistant.ai.AiService;
import com.personalassistant.dto.AiCaptureAnalysis;
import com.personalassistant.dto.CaptureResponse;
import com.personalassistant.dto.CreateCaptureRequest;
import com.personalassistant.entity.AiProcessingStatus;
import com.personalassistant.entity.Capture;
import com.personalassistant.entity.CaptureType;
import com.personalassistant.entity.User;
import com.personalassistant.exception.AiProcessingException;
import com.personalassistant.exception.CaptureNotFoundException;
import com.personalassistant.exception.UserNotFoundException;
import com.personalassistant.mapper.CaptureMapper;
import com.personalassistant.repository.CaptureRepository;
import com.personalassistant.repository.UserRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
@Transactional
public class CaptureService {

    private final CaptureRepository captureRepository;
    private final UserRepository userRepository;
    private final CaptureMapper captureMapper;
    private final AiService aiService;
    private final TaskService taskService;

    public CaptureService(
            CaptureRepository captureRepository,
            UserRepository userRepository,
            CaptureMapper captureMapper,
            AiService aiService,
            TaskService taskService
    ) {
        this.captureRepository = captureRepository;
        this.userRepository = userRepository;
        this.captureMapper = captureMapper;
        this.aiService = aiService;
        this.taskService = taskService;
    }

    public CaptureResponse createCapture(
            CreateCaptureRequest request,
            UUID userId
    ) {

        validateCaptureRequest(request);

        User user = userRepository.findById(userId)
                .orElseThrow(() ->
                        new UserNotFoundException("User not found")
                );

        Capture capture = captureMapper.toEntity(request);

        capture.setUser(user);
        capture.setAiStatus(AiProcessingStatus.PENDING);

        Capture savedCapture = captureRepository.save(capture);

        savedCapture.setAiStatus(AiProcessingStatus.PROCESSING);
        captureRepository.save(savedCapture);

        AiCaptureAnalysis analysis;

        try {

            String aiInput = buildAiInput(savedCapture);

            analysis = aiService.analyzeCapture(aiInput);

            // Store AI-generated summary
            savedCapture.setAiSummary(analysis.summary());

        } catch (AiProcessingException exception) {

            savedCapture.setAiStatus(AiProcessingStatus.FAILED);
            savedCapture.setAiError(exception.getMessage());

            captureRepository.save(savedCapture);

            return captureMapper.toResponse(savedCapture);
        }

        taskService.createTasksFromAiAnalysis(
                analysis,
                userId,
                savedCapture.getId()
        );

        savedCapture.setAiStatus(AiProcessingStatus.COMPLETED);
        savedCapture.setAiError(null);

        captureRepository.save(savedCapture);

        return captureMapper.toResponse(savedCapture);
    }

    @Transactional(readOnly = true)
    public List<CaptureResponse> getMyCaptures(UUID userId) {

        return captureRepository.findByUserId(userId)
                .stream()
                .map(captureMapper::toResponse)
                .toList();
    }

    @Transactional(readOnly = true)
    public CaptureResponse getCapture(
            UUID captureId,
            UUID userId
    ) {

        Capture capture = captureRepository
                .findByIdAndUserId(captureId, userId)
                .orElseThrow(() ->
                        new CaptureNotFoundException("Capture not found")
                );

        return captureMapper.toResponse(capture);
    }

    public CaptureResponse retryAiProcessing(
            UUID captureId,
            UUID userId
    ) {

        Capture capture = captureRepository
                .findByIdAndUserId(captureId, userId)
                .orElseThrow(() ->
                        new CaptureNotFoundException("Capture not found")
                );

        if (capture.getAiStatus() != AiProcessingStatus.FAILED) {
            throw new IllegalStateException(
                    "Only failed captures can be retried"
            );
        }

        capture.setAiError(null);
        capture.setAiStatus(AiProcessingStatus.PROCESSING);

        captureRepository.save(capture);

        AiCaptureAnalysis analysis;

        try {

            String aiInput = buildAiInput(capture);

            analysis = aiService.analyzeCapture(aiInput);

            // Store AI-generated summary after retry
            capture.setAiSummary(analysis.summary());

        } catch (AiProcessingException exception) {

            capture.setAiStatus(AiProcessingStatus.FAILED);
            capture.setAiError(exception.getMessage());

            captureRepository.save(capture);

            return captureMapper.toResponse(capture);
        }

        taskService.createTasksFromAiAnalysis(
                analysis,
                userId,
                capture.getId()
        );

        capture.setAiStatus(AiProcessingStatus.COMPLETED);
        capture.setAiError(null);

        captureRepository.save(capture);

        return captureMapper.toResponse(capture);
    }

    public void deleteCapture(
            UUID captureId,
            UUID userId
    ) {

        Capture capture = captureRepository
                .findByIdAndUserId(captureId, userId)
                .orElseThrow(() ->
                        new CaptureNotFoundException("Capture not found")
                );

        captureRepository.delete(capture);
    }

    private void validateCaptureRequest(
            CreateCaptureRequest request
    ) {

        CaptureType type = request.getType();

        switch (type) {

            case TEXT -> {
                if (request.getContent() == null
                        || request.getContent().isBlank()) {

                    throw new IllegalArgumentException(
                            "Content is required for TEXT captures"
                    );
                }
            }

            case URL -> {
                if (request.getSourceUrl() == null
                        || request.getSourceUrl().isBlank()) {

                    throw new IllegalArgumentException(
                            "Source URL is required for URL captures"
                    );
                }
            }

            case VOICE ->
                    throw new IllegalArgumentException(
                            "Voice capture is not supported yet"
                    );

            case IMAGE ->
                    throw new IllegalArgumentException(
                            "Image capture is not supported yet"
                    );
        }
    }

    private String buildAiInput(Capture capture) {

        return switch (capture.getType()) {

            case TEXT ->
                    capture.getContent();

            case URL ->
                    """
                    User context:
                    %s

                    Source URL:
                    %s
                    """.formatted(
                            capture.getContent(),
                            capture.getSourceUrl()
                    );

            case VOICE ->
                    capture.getTranscript();

            case IMAGE ->
                    throw new UnsupportedOperationException(
                            "Image AI analysis is not implemented yet"
                    );
        };
    }
}