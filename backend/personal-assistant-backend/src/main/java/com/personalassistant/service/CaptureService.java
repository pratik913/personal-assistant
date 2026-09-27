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
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.UUID;

@Service
@Transactional
public class CaptureService {

    private static final long MAX_IMAGE_SIZE = 5 * 1024 * 1024;

    private final CaptureRepository captureRepository;
    private final UserRepository userRepository;
    private final CaptureMapper captureMapper;
    private final AiService aiService;
    private final TaskService taskService;
    private final S3StorageService s3StorageService;

    public CaptureService(
            CaptureRepository captureRepository,
            UserRepository userRepository,
            CaptureMapper captureMapper,
            AiService aiService,
            TaskService taskService,
            S3StorageService s3StorageService
    ) {
        this.captureRepository = captureRepository;
        this.userRepository = userRepository;
        this.captureMapper = captureMapper;
        this.aiService = aiService;
        this.taskService = taskService;
        this.s3StorageService = s3StorageService;
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

    /**
     * Creates an IMAGE capture and uploads the image to private S3 storage.
     *
     * AI vision processing is intentionally not performed yet.
     * That will be implemented in the next step.
     */
    public CaptureResponse createImageCapture(
            MultipartFile file,
            String content,
            UUID userId
    ) {

        validateImage(file);

        User user = userRepository.findById(userId)
                .orElseThrow(() ->
                        new UserNotFoundException("User not found")
                );

        Capture capture = new Capture();

        capture.setType(CaptureType.IMAGE);
        capture.setContent(
                content != null && !content.isBlank()
                        ? content.trim()
                        : null
        );
        capture.setUser(user);
        capture.setAiStatus(AiProcessingStatus.PENDING);

        Capture savedCapture = captureRepository.save(capture);

        String extension = getImageExtension(file);
        String key = "captures/" + userId + "/" + savedCapture.getId()
                + "/screenshot." + extension;

        try {

            s3StorageService.upload(key, file);

            savedCapture.setStorageUrl(key);

            savedCapture.setAiStatus(AiProcessingStatus.PENDING);
            savedCapture.setAiError(null);

            captureRepository.save(savedCapture);

            return captureMapper.toResponse(savedCapture);

        } catch (RuntimeException exception) {

            savedCapture.setAiStatus(AiProcessingStatus.FAILED);
            savedCapture.setAiError(
                    "Screenshot upload failed. Please try again."
            );

            captureRepository.save(savedCapture);

            throw exception;
        }
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

        if (capture.getStorageUrl() != null
                && !capture.getStorageUrl().isBlank()) {

            try {
                s3StorageService.delete(capture.getStorageUrl());
            } catch (RuntimeException exception) {
                // Keep database deletion independent from S3 cleanup.
                // The object can be cleaned up separately if required.
            }
        }

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
                            "Use the image upload endpoint for IMAGE captures"
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

    private void validateImage(MultipartFile file) {

        if (file == null || file.isEmpty()) {
            throw new IllegalArgumentException(
                    "Screenshot file is required."
            );
        }

        if (file.getSize() > MAX_IMAGE_SIZE) {
            throw new IllegalArgumentException(
                    "Screenshot must not exceed 5 MB."
            );
        }

        String contentType = file.getContentType();

        if (contentType == null
                || !contentType.equalsIgnoreCase("image/jpeg")
                && !contentType.equalsIgnoreCase("image/png")
                && !contentType.equalsIgnoreCase("image/webp")) {

            throw new IllegalArgumentException(
                    "Only JPG, PNG, and WebP screenshots are supported."
            );
        }
    }

    private String getImageExtension(MultipartFile file) {

        String contentType = file.getContentType();

        return switch (contentType == null ? "" : contentType.toLowerCase()) {
            case "image/png" -> "png";
            case "image/webp" -> "webp";
            case "image/jpeg" -> "jpg";
            default -> throw new IllegalArgumentException(
                    "Unsupported image type."
            );
        };
    }
}