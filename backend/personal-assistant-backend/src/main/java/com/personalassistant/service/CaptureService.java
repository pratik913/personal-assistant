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

import java.io.IOException;
import java.io.InputStream;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.List;
import java.util.UUID;

@Service
@Transactional
public class CaptureService {

    private static final long MAX_IMAGE_SIZE =
            5 * 1024 * 1024;

    private static final long MAX_VOICE_SIZE =
            25 * 1024 * 1024;

    private final CaptureRepository captureRepository;

    private final UserRepository userRepository;

    private final CaptureMapper captureMapper;

    private final AiService aiService;

    private final MediaAiService mediaAiService;

    private final TaskService taskService;

    private final S3StorageService s3StorageService;

    public CaptureService(
            CaptureRepository captureRepository,
            UserRepository userRepository,
            CaptureMapper captureMapper,
            AiService aiService,
            MediaAiService mediaAiService,
            TaskService taskService,
            S3StorageService s3StorageService
    ) {

        this.captureRepository =
                captureRepository;

        this.userRepository =
                userRepository;

        this.captureMapper =
                captureMapper;

        this.aiService =
                aiService;

        this.mediaAiService =
                mediaAiService;

        this.taskService =
                taskService;

        this.s3StorageService =
                s3StorageService;
    }

    // ============================================================
    // TEXT / URL
    // ============================================================

    public CaptureResponse createCapture(
            CreateCaptureRequest request,
            UUID userId
    ) {

        validateCaptureRequest(request);

        User user =
                getUser(userId);

        Capture capture =
                captureMapper.toEntity(request);

        capture.setUser(user);

        capture.setAiStatus(
                AiProcessingStatus.PENDING
        );

        Capture savedCapture =
                captureRepository.save(capture);

        return processTextOrUrlCapture(
                savedCapture,
                userId
        );
    }

    // ============================================================
    // IMAGE
    // ============================================================

    public CaptureResponse createImageCapture(
            MultipartFile file,
            String content,
            UUID userId
    ) {

        validateImage(file);

        User user =
                getUser(userId);

        Capture capture =
                new Capture();

        capture.setType(
                CaptureType.IMAGE
        );

        capture.setContent(
                normalizeOptional(content)
        );

        capture.setUser(user);

        capture.setAiStatus(
                AiProcessingStatus.PENDING
        );

        Capture savedCapture =
                captureRepository.save(capture);

        String extension =
                getImageExtension(file);

        String key =
                "captures/"
                        + userId
                        + "/"
                        + savedCapture.getId()
                        + "/screenshot."
                        + extension;

        try {

            /*
             * 1. Store original screenshot privately in S3.
             */
            s3StorageService.upload(
                    key,
                    file
            );

            savedCapture.setStorageUrl(key);

            savedCapture.setAiStatus(
                    AiProcessingStatus.PROCESSING
            );

            captureRepository.save(
                    savedCapture
            );

            /*
             * 2. Read the private S3 object.
             */
            try (
                    InputStream imageStream =
                            s3StorageService.download(key)
            ) {

                /*
                 * 3. Vision model understands screenshot.
                 */
                String visualDescription =
                        mediaAiService.analyzeImage(
                                imageStream,
                                file.getContentType(),
                                savedCapture.getContent()
                        );

                /*
                 * 4. Existing capture AI converts the
                 *    understanding into structured tasks.
                 */
                String aiInput =
                        buildMediaTaskInput(
                                "Screenshot analysis:",
                                visualDescription,
                                savedCapture.getContent()
                        );

                AiCaptureAnalysis analysis =
                        aiService.analyzeCapture(
                                buildAiCaptureInput(
                                        aiInput,
                                        user
                                )
                        );

                savedCapture.setAiSummary(
                        analysis.summary()
                );

                /*
                 * 5. Existing task creation pipeline.
                 */
                taskService.createTasksFromAiAnalysis(
                        analysis,
                        userId,
                        savedCapture.getId()
                );

                savedCapture.setAiStatus(
                        AiProcessingStatus.COMPLETED
                );

                savedCapture.setAiError(null);

                captureRepository.save(
                        savedCapture
                );

                return captureMapper.toResponse(
                        savedCapture
                );
            }

        } catch (AiProcessingException exception) {

            markFailed(
                    savedCapture,
                    exception.getMessage()
            );

            return captureMapper.toResponse(
                    savedCapture
            );

        } catch (IOException exception) {

            markFailed(
                    savedCapture,
                    "Unable to process the screenshot."
            );

            return captureMapper.toResponse(
                    savedCapture
            );

        } catch (RuntimeException exception) {

            markFailed(
                    savedCapture,
                    "Screenshot processing failed. Please try again."
            );

            throw exception;
        }
    }

    // ============================================================
    // VOICE
    // ============================================================

    public CaptureResponse createVoiceCapture(
            MultipartFile file,
            String content,
            UUID userId
    ) {

        validateVoice(file);

        User user =
                getUser(userId);

        Capture capture =
                new Capture();

        capture.setType(
                CaptureType.VOICE
        );

        capture.setContent(
                normalizeOptional(content)
        );

        capture.setUser(user);

        capture.setAiStatus(
                AiProcessingStatus.PENDING
        );

        Capture savedCapture =
                captureRepository.save(capture);

        String extension =
                getVoiceExtension(file);

        String key =
                "captures/"
                        + userId
                        + "/"
                        + savedCapture.getId()
                        + "/voice."
                        + extension;

        try {

            /*
             * 1. Store voice recording privately in S3.
             */
            s3StorageService.upload(
                    key,
                    file
            );

            savedCapture.setStorageUrl(key);

            savedCapture.setAiStatus(
                    AiProcessingStatus.PROCESSING
            );

            captureRepository.save(
                    savedCapture
            );

            /*
             * 2. Read private S3 object.
             */
            try (
                    InputStream audioStream =
                            s3StorageService.download(key)
            ) {

                /*
                 * 3. Speech → text.
                 */
                String transcript =
                        mediaAiService.transcribe(
                                audioStream,
                                file.getContentType(),
                                safeFilename(
                                        file.getOriginalFilename(),
                                        "voice." + extension
                                )
                        );

                savedCapture.setTranscript(
                        transcript
                );

                /*
                 * 4. Feed transcript into existing
                 *    capture understanding pipeline.
                 */
                String aiInput =
                        buildMediaTaskInput(
                                "Voice transcript:",
                                transcript,
                                savedCapture.getContent()
                        );

                AiCaptureAnalysis analysis =
                        aiService.analyzeCapture(
                                buildAiCaptureInput(
                                        aiInput,
                                        user
                                )
                        );

                savedCapture.setAiSummary(
                        analysis.summary()
                );

                /*
                 * 5. Existing task creation.
                 */
                taskService.createTasksFromAiAnalysis(
                        analysis,
                        userId,
                        savedCapture.getId()
                );

                savedCapture.setAiStatus(
                        AiProcessingStatus.COMPLETED
                );

                savedCapture.setAiError(null);

                captureRepository.save(
                        savedCapture
                );

                return captureMapper.toResponse(
                        savedCapture
                );
            }

        } catch (AiProcessingException exception) {

            markFailed(
                    savedCapture,
                    exception.getMessage()
            );

            return captureMapper.toResponse(
                    savedCapture
            );

        } catch (IOException exception) {

            markFailed(
                    savedCapture,
                    "Unable to process the voice recording."
            );

            return captureMapper.toResponse(
                    savedCapture
            );

        } catch (RuntimeException exception) {

            markFailed(
                    savedCapture,
                    "Voice processing failed. Please try again."
            );

            throw exception;
        }
    }

    // ============================================================
    // RETRIEVE
    // ============================================================

    @Transactional(readOnly = true)
    public List<CaptureResponse> getMyCaptures(
            UUID userId
    ) {

        return captureRepository
                .findByUserId(userId)
                .stream()
                .map(captureMapper::toResponse)
                .toList();
    }

    @Transactional(readOnly = true)
    public CaptureResponse getCapture(
            UUID captureId,
            UUID userId
    ) {

        Capture capture =
                captureRepository
                        .findByIdAndUserId(
                                captureId,
                                userId
                        )
                        .orElseThrow(() ->
                                new CaptureNotFoundException(
                                        "Capture not found"
                                )
                        );

        return captureMapper.toResponse(
                capture
        );
    }

    // ============================================================
    // RETRY
    // ============================================================

    public CaptureResponse retryAiProcessing(
            UUID captureId,
            UUID userId
    ) {

        Capture capture =
                captureRepository
                        .findByIdAndUserId(
                                captureId,
                                userId
                        )
                        .orElseThrow(() ->
                                new CaptureNotFoundException(
                                        "Capture not found"
                                )
                        );

        User user =
                getUser(userId);

        if (
                capture.getAiStatus()
                        != AiProcessingStatus.FAILED
        ) {

            throw new IllegalStateException(
                    "Only failed captures can be retried"
            );
        }

        capture.setAiError(null);

        capture.setAiStatus(
                AiProcessingStatus.PROCESSING
        );

        captureRepository.save(
                capture
        );

        try {

            AiCaptureAnalysis analysis;

            if (
                    capture.getType()
                            == CaptureType.IMAGE
            ) {

                if (
                        capture.getStorageUrl()
                                == null
                                || capture.getStorageUrl().isBlank()
                ) {

                    throw new AiProcessingException(
                            com.personalassistant.exception.AiErrorType.INVALID_RESPONSE,
                            "Screenshot storage is missing."
                    );
                }

                try (
                        InputStream imageStream =
                                s3StorageService.download(
                                        capture.getStorageUrl()
                                )
                ) {

                    String visualDescription =
                            mediaAiService.analyzeImage(
                                    imageStream,
                                    inferImageContentType(
                                            capture.getStorageUrl()
                                    ),
                                    capture.getContent()
                            );

                    analysis =
                            aiService.analyzeCapture(
                                    buildAiCaptureInput(
                                            buildMediaTaskInput(
                                                    "Screenshot analysis:",
                                                    visualDescription,
                                                    capture.getContent()
                                            ),
                                            user
                                    )
                            );
                }

            } else if (
                    capture.getType()
                            == CaptureType.VOICE
            ) {

                if (
                        capture.getTranscript()
                                == null
                                || capture.getTranscript().isBlank()
                ) {

                    throw new AiProcessingException(
                            com.personalassistant.exception.AiErrorType.INVALID_RESPONSE,
                            "Voice transcript is missing."
                    );
                }

                analysis =
                        aiService.analyzeCapture(
                                buildAiCaptureInput(
                                        buildMediaTaskInput(
                                                "Voice transcript:",
                                                capture.getTranscript(),
                                                capture.getContent()
                                        ),
                                        user
                                )
                        );

            } else {

                analysis =
                        aiService.analyzeCapture(
                                buildAiInput(
                                        capture,
                                        user
                                )
                        );
            }

            capture.setAiSummary(
                    analysis.summary()
            );

            taskService.createTasksFromAiAnalysis(
                    analysis,
                    userId,
                    capture.getId()
            );

            capture.setAiStatus(
                    AiProcessingStatus.COMPLETED
            );

            capture.setAiError(null);

            captureRepository.save(
                    capture
            );

            return captureMapper.toResponse(
                    capture
            );

        } catch (AiProcessingException exception) {

            markFailed(
                    capture,
                    exception.getMessage()
            );

            return captureMapper.toResponse(
                    capture
            );

        } catch (IOException exception) {

            markFailed(
                    capture,
                    "Unable to read the stored capture media."
            );

            return captureMapper.toResponse(
                    capture
            );
        }
    }

    // ============================================================
    // DELETE
    // ============================================================

    public void deleteCapture(
            UUID captureId,
            UUID userId
    ) {

        Capture capture =
                captureRepository
                        .findByIdAndUserId(
                                captureId,
                                userId
                        )
                        .orElseThrow(() ->
                                new CaptureNotFoundException(
                                        "Capture not found"
                                )
                        );

        if (
                capture.getStorageUrl() != null
                        && !capture.getStorageUrl().isBlank()
        ) {

            try {

                s3StorageService.delete(
                        capture.getStorageUrl()
                );

            } catch (RuntimeException ignored) {

                // Database deletion remains independent
                // from S3 cleanup.
            }
        }

        captureRepository.delete(
                capture
        );
    }

    // ============================================================
    // STANDARD AI
    // ============================================================

    private CaptureResponse processTextOrUrlCapture(
            Capture capture,
            UUID userId
    ) {

        capture.setAiStatus(
                AiProcessingStatus.PROCESSING
        );

        captureRepository.save(
                capture
        );

        try {

            User user =
                    getUser(userId);

            AiCaptureAnalysis analysis =
                    aiService.analyzeCapture(
                            buildAiInput(
                                    capture,
                                    user
                            )
                    );

            capture.setAiSummary(
                    analysis.summary()
            );

            taskService.createTasksFromAiAnalysis(
                    analysis,
                    userId,
                    capture.getId()
            );

            capture.setAiStatus(
                    AiProcessingStatus.COMPLETED
            );

            capture.setAiError(null);

            captureRepository.save(
                    capture
            );

            return captureMapper.toResponse(
                    capture
            );

        } catch (AiProcessingException exception) {

            markFailed(
                    capture,
                    exception.getMessage()
            );

            return captureMapper.toResponse(
                    capture
            );
        }
    }

    // ============================================================
    // VALIDATION
    // ============================================================

    private void validateCaptureRequest(
            CreateCaptureRequest request
    ) {

        if (
                request == null
                        || request.getType() == null
        ) {

            throw new IllegalArgumentException(
                    "Capture type is required."
            );
        }

        switch (request.getType()) {

            case TEXT -> {

                if (
                        request.getContent() == null
                                || request.getContent().isBlank()
                ) {

                    throw new IllegalArgumentException(
                            "Content is required for TEXT captures"
                    );
                }
            }

            case URL -> {

                if (
                        request.getSourceUrl() == null
                                || request.getSourceUrl().isBlank()
                ) {

                    throw new IllegalArgumentException(
                            "Source URL is required for URL captures"
                    );
                }
            }

            case VOICE ->
                    throw new IllegalArgumentException(
                            "Use the voice upload endpoint for VOICE captures"
                    );

            case IMAGE ->
                    throw new IllegalArgumentException(
                            "Use the image upload endpoint for IMAGE captures"
                    );
        }
    }

    private void validateImage(
            MultipartFile file
    ) {

        if (
                file == null
                        || file.isEmpty()
        ) {

            throw new IllegalArgumentException(
                    "Screenshot file is required."
            );
        }

        if (
                file.getSize()
                        > MAX_IMAGE_SIZE
        ) {

            throw new IllegalArgumentException(
                    "Screenshot must not exceed 5 MB."
            );
        }

        String contentType =
                file.getContentType();

        if (
                contentType == null
                        || !(
                        contentType.equalsIgnoreCase(
                                "image/jpeg"
                        )
                                || contentType.equalsIgnoreCase(
                                "image/png"
                        )
                                || contentType.equalsIgnoreCase(
                                "image/webp"
                        )
                )
        ) {

            throw new IllegalArgumentException(
                    "Only JPG, PNG, and WebP screenshots are supported."
            );
        }
    }

    private void validateVoice(
            MultipartFile file
    ) {

        if (
                file == null
                        || file.isEmpty()
        ) {

            throw new IllegalArgumentException(
                    "Voice recording is required."
            );
        }

        if (
                file.getSize()
                        > MAX_VOICE_SIZE
        ) {

            throw new IllegalArgumentException(
                    "Voice recording must not exceed 25 MB."
            );
        }

        String contentType =
                file.getContentType();

        if (
                contentType == null
                        || !isSupportedVoiceType(
                        contentType
                )
        ) {

            throw new IllegalArgumentException(
                    "Supported voice formats are WebM, OGG, MP4, MP3, and WAV."
            );
        }
    }

    private boolean isSupportedVoiceType(
            String contentType
    ) {

        String normalized =
                contentType.toLowerCase();

        return normalized.equals(
                "audio/webm"
        )
                || normalized.startsWith(
                "audio/webm;"
        )
                || normalized.equals(
                "audio/ogg"
        )
                || normalized.equals(
                "audio/mp4"
        )
                || normalized.equals(
                "audio/mpeg"
        )
                || normalized.equals(
                "audio/wav"
        )
                || normalized.equals(
                "audio/x-wav"
        );
    }

    // ============================================================
    // AI INPUT
    // ============================================================
    private String buildAiInput(
            Capture capture,
            User user
    ) {

        String content;

        if (capture.getType() == CaptureType.TEXT) {

            content =
                    capture.getContent();

        } else if (capture.getType() == CaptureType.URL) {

            String urlType =
                    isInstagramUrl(capture.getSourceUrl())
                            ? "INSTAGRAM_REEL"
                            : "GENERIC_URL";

            content =
                    """
                    Capture type:
                    %s
    
                    Source URL:
                    %s
    
                    User context:
                    %s
                    """.formatted(
                            urlType,
                            capture.getSourceUrl(),
                            capture.getContent() != null
                                    && !capture.getContent().isBlank()
                                    ? capture.getContent()
                                    : "No additional context provided."
                    );

        } else if (capture.getType() == CaptureType.VOICE) {

            content =
                    capture.getTranscript();

        } else {

            throw new UnsupportedOperationException(
                    "Image AI analysis must use the media pipeline."
            );
        }

        return buildAiCaptureInput(
                content,
                user
        );
    }

    private boolean isInstagramUrl(
            String sourceUrl
    ) {

        if (
                sourceUrl == null
                        || sourceUrl.isBlank()
        ) {
            return false;
        }

        String normalized =
                sourceUrl
                        .trim()
                        .toLowerCase();

        return normalized.contains(
                "instagram.com/reel/"
        )
                || normalized.contains(
                "instagram.com/reels/"
        );
    }

    private String buildAiCaptureInput(
            String content,
            User user
    ) {

        String timezone =
                user.getTimezone();

        LocalDate currentLocalDate;

        try {

            currentLocalDate =
                    LocalDate.now(
                            ZoneId.of(timezone)
                    );

        } catch (RuntimeException exception) {

            currentLocalDate =
                    LocalDate.now();
        }

        return """
                User timezone:
                %s

                Current local date:
                %s

                User capture:
                %s
                """.formatted(
                timezone,
                currentLocalDate,
                content
        );
    }

    private String buildMediaTaskInput(
            String label,
            String primaryContent,
            String userContext
    ) {

        return """
                %s
                %s

                User context:
                %s
                """.formatted(
                label,
                primaryContent,
                userContext != null
                        && !userContext.isBlank()
                        ? userContext
                        : "No additional context provided."
        );
    }

    // ============================================================
    // FILE HELPERS
    // ============================================================

    private String getImageExtension(
            MultipartFile file
    ) {

        return switch (
                file.getContentType() == null
                        ? ""
                        : file.getContentType().toLowerCase()
                ) {

            case "image/png" ->
                    "png";

            case "image/webp" ->
                    "webp";

            case "image/jpeg" ->
                    "jpg";

            default ->
                    throw new IllegalArgumentException(
                            "Unsupported image type."
                    );
        };
    }

    private String getVoiceExtension(
            MultipartFile file
    ) {

        String contentType =
                file.getContentType() == null
                        ? ""
                        : file.getContentType().toLowerCase();

        if (
                contentType.startsWith(
                        "audio/webm"
                )
        ) {
            return "webm";
        }

        if (
                contentType.equals(
                        "audio/ogg"
                )
        ) {
            return "ogg";
        }

        if (
                contentType.equals(
                        "audio/mp4"
                )
        ) {
            return "mp4";
        }

        if (
                contentType.equals(
                        "audio/mpeg"
                )
        ) {
            return "mp3";
        }

        if (
                contentType.equals(
                        "audio/wav"
                )
                        || contentType.equals(
                        "audio/x-wav"
                )
        ) {
            return "wav";
        }

        throw new IllegalArgumentException(
                "Unsupported voice type."
        );
    }

    private String inferImageContentType(
            String key
    ) {

        String lower =
                key.toLowerCase();

        if (
                lower.endsWith(".png")
        ) {
            return "image/png";
        }

        if (
                lower.endsWith(".webp")
        ) {
            return "image/webp";
        }

        return "image/jpeg";
    }

    private String safeFilename(
            String filename,
            String fallback
    ) {

        return filename == null
                || filename.isBlank()
                ? fallback
                : filename;
    }

    private String normalizeOptional(
            String value
    ) {

        return value == null
                || value.isBlank()
                ? null
                : value.trim();
    }

    private User getUser(
            UUID userId
    ) {

        return userRepository
                .findById(userId)
                .orElseThrow(() ->
                        new UserNotFoundException(
                                "User not found"
                        )
                );
    }

    private void markFailed(
            Capture capture,
            String message
    ) {

        capture.setAiStatus(
                AiProcessingStatus.FAILED
        );

        capture.setAiError(
                message == null
                        || message.isBlank()
                        ? "AI processing failed. Please try again."
                        : message
        );

        captureRepository.save(
                capture
        );
    }


}