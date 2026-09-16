package com.personalassistant.service;

import com.personalassistant.ai.AiService;
import com.personalassistant.dto.AiCaptureAnalysis;
import com.personalassistant.dto.CaptureResponse;
import com.personalassistant.dto.CreateCaptureRequest;
import com.personalassistant.entity.Capture;
import com.personalassistant.entity.User;
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

        User user = userRepository.findById(userId)
                .orElseThrow(() ->
                        new UserNotFoundException("User not found")
                );

        Capture capture = captureMapper.toEntity(request);

        capture.setUser(user);

        Capture savedCapture = captureRepository.save(capture);

        /*
         * AI processing is intentionally isolated from capture creation.
         *
         * If AI fails, the user's capture should still be saved.
         */
        AiCaptureAnalysis analysis;

        try {

            String aiInput = buildAiInput(savedCapture);

            analysis = aiService.analyzeCapture(aiInput);

        } catch (RuntimeException exception) {

            /*
             * V1 behavior:
             * AI failure should not cause capture creation to fail.
             *
             * The capture is already saved, so we simply return it.
             */
            return captureMapper.toResponse(savedCapture);
        }

        /*
         * AI successfully analyzed the capture.
         * Convert AI suggestions into actual tasks.
         */
        taskService.createTasksFromAiAnalysis(
                analysis,
                userId,
                savedCapture.getId()
        );

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