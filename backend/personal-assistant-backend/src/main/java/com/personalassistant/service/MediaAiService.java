package com.personalassistant.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.openai.client.OpenAIClient;
import com.openai.client.okhttp.OpenAIOkHttpClient;
import com.openai.models.audio.transcriptions.TranscriptionCreateParams;
import com.openai.models.responses.ResponseCreateParams;
import com.openai.models.responses.ResponseInputImage;
import com.openai.models.responses.ResponseInputItem;
import com.personalassistant.exception.AiErrorType;
import com.personalassistant.exception.AiProcessingException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatusCode;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClientResponseException;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Base64;
import java.util.List;

@Service
public class MediaAiService {

    private static final String VISION_MODEL =
            "gpt-5";

    private static final String TRANSCRIPTION_MODEL =
            "gpt-4o-mini-transcribe";

    private final ObjectMapper objectMapper;

    private final String apiKey;

    private final OpenAIClient openAIClient;

    public MediaAiService(
            ObjectMapper objectMapper,
            @Value("${openai.api-key:}") String apiKey
    ) {

        this.objectMapper = objectMapper;
        this.apiKey = apiKey;

        this.openAIClient =
                OpenAIOkHttpClient
                        .builder()
                        .apiKey(apiKey)
                        .build();
    }

    /**
     * Analyzes an image stored in S3 using the OpenAI Responses API.
     */
    public String analyzeImage(
            InputStream imageStream,
            String contentType,
            String userContext
    ) {

        try {

            requireApiKey();

            if (
                    contentType == null
                            || contentType.isBlank()
            ) {

                throw new AiProcessingException(
                        AiErrorType.UNSUPPORTED,
                        "Screenshot content type is missing."
                );
            }

            byte[] imageBytes =
                    imageStream.readAllBytes();

            if (imageBytes.length == 0) {

                throw new AiProcessingException(
                        AiErrorType.INVALID_RESPONSE,
                        "Screenshot is empty."
                );
            }

            String dataUrl =
                    "data:"
                            + contentType
                            + ";base64,"
                            + Base64.getEncoder()
                            .encodeToString(imageBytes);

            String prompt = """
                    You are analyzing a screenshot captured by a
                    personal productivity assistant.

                    Identify what the screenshot is about and what
                    actionable intention it may represent.

                    Focus on:
                    - What is visible in the screenshot
                    - What the user appears to be trying to accomplish
                    - Any task, project, reminder, or actionable intention
                    - Important text visible in the screenshot

                    Do not invent actions that are not supported by
                    the screenshot or user context.

                    If the screenshot is not actionable, say that clearly.

                    User-provided context:
                    %s
                    """.formatted(
                    userContext != null
                            && !userContext.isBlank()
                            ? userContext
                            : "No additional context provided."
            );

            /*
             * openai-java 4.63.1 requires the image detail field.
             */
            ResponseInputImage image =
                    ResponseInputImage.builder()
                            .imageUrl(dataUrl)
                            .detail(
                                    ResponseInputImage.Detail.HIGH
                            )
                            .build();

            ResponseInputItem.Message message =
                    ResponseInputItem.Message.builder()
                            .role(
                                    ResponseInputItem.Message.Role.USER
                            )
                            .addInputTextContent(prompt)
                            .addContent(image)
                            .build();

            ResponseInputItem inputItem =
                    ResponseInputItem.ofMessage(message);

            ResponseCreateParams params =
                    ResponseCreateParams.builder()
                            .model(VISION_MODEL)
                            .inputOfResponse(
                                    List.of(inputItem)
                            )
                            .build();

            var response =
                    openAIClient
                            .responses()
                            .create(params);

            JsonNode responseJson =
                    objectMapper.readTree(
                            objectMapper.writeValueAsString(response)
                    );

            return extractOutputText(responseJson);

        } catch (AiProcessingException exception) {

            logImageAiFailure(exception);

            throw exception;

        } catch (RestClientResponseException exception) {

            logImageAiFailure(exception);

            throw classifyHttpException(exception);

        } catch (IOException exception) {

            logImageAiFailure(exception);

            throw new AiProcessingException(
                    AiErrorType.TEMPORARY,
                    "Unable to read the screenshot for AI analysis.",
                    exception
            );

        } catch (RuntimeException exception) {

            logImageAiFailure(exception);

            throw classifyException(exception);
        }
    }

    /**
     * Converts stored audio into text using the OpenAI Java SDK.
     *
     * The audio is temporarily written to a local file because the
     * OpenAI Java SDK transcription API accepts a Path-based file input.
     */
    public String transcribe(
            InputStream audioStream,
            String contentType,
            String filename
    ) {

        Path temporaryAudioFile = null;

        try {

            requireApiKey();

            final String resolvedFilename =
                    filename == null || filename.isBlank()
                            ? "voice.webm"
                            : filename;

            byte[] audioBytes =
                    audioStream.readAllBytes();

            if (audioBytes.length == 0) {

                throw new AiProcessingException(
                        AiErrorType.INVALID_RESPONSE,
                        "Voice recording is empty."
                );
            }

            /*
             * Determine a safe extension for the temporary file.
             */
            String extension =
                    extractExtension(resolvedFilename);

            temporaryAudioFile =
                    Files.createTempFile(
                            "mindmate-voice-",
                            extension
                    );

            Files.write(
                    temporaryAudioFile,
                    audioBytes
            );

            System.out.println();

            System.out.println(
                    "========== VOICE TRANSCRIPTION REQUEST =========="
            );

            System.out.println(
                    "Content type: " + contentType
            );

            System.out.println(
                    "Filename: " + resolvedFilename
            );

            System.out.println(
                    "Audio size: " + audioBytes.length + " bytes"
            );

            System.out.println(
                    "Model: " + TRANSCRIPTION_MODEL
            );

            System.out.println(
                    "================================================="
            );

            /*
             * OpenAI Java SDK transcription request.
             */
            TranscriptionCreateParams params =
                    TranscriptionCreateParams.builder()
                            .file(temporaryAudioFile)
                            .model(TRANSCRIPTION_MODEL)
                            .build();

            var result =
                    openAIClient
                            .audio()
                            .transcriptions()
                            .create(params);

            /*
             * Convert the SDK response into the normal transcription
             * representation.
             */
            String transcript =
                    result
                            .asTranscription()
                            .text();

            if (
                    transcript == null
                            || transcript.isBlank()
            ) {

                throw new AiProcessingException(
                        AiErrorType.INVALID_RESPONSE,
                        "AI returned an empty voice transcription."
                );
            }

            System.out.println(
                    "========== VOICE TRANSCRIPTION SUCCESS =========="
            );

            System.out.println(
                    "Transcript length: "
                            + transcript.length()
            );

            System.out.println(
                    "=================================================="
            );

            return transcript.trim();

        } catch (AiProcessingException exception) {

            logVoiceTranscriptionFailure(exception);

            throw exception;

        } catch (IOException exception) {

            logVoiceTranscriptionFailure(exception);

            throw new AiProcessingException(
                    AiErrorType.TEMPORARY,
                    "Unable to read the voice recording for transcription.",
                    exception
            );

        } catch (RuntimeException exception) {

            logVoiceTranscriptionFailure(exception);

            throw classifyException(exception);

        } finally {

            /*
             * Always remove the temporary audio file.
             */
            if (temporaryAudioFile != null) {

                try {

                    Files.deleteIfExists(
                            temporaryAudioFile
                    );

                } catch (IOException exception) {

                    System.err.println(
                            "Unable to delete temporary voice file: "
                                    + temporaryAudioFile
                    );
                }
            }
        }
    }

    /**
     * Extracts text from an OpenAI Responses API response.
     */
    private String extractOutputText(
            JsonNode response
    ) {

        if (response == null) {

            throw new AiProcessingException(
                    AiErrorType.INVALID_RESPONSE,
                    "AI returned an empty image analysis response."
            );
        }

        /*
         * Preferred Responses API output.
         */
        JsonNode directOutputText =
                response.get("output_text");

        if (
                directOutputText != null
                        && directOutputText.isTextual()
                        && !directOutputText.asText().isBlank()
        ) {

            return directOutputText
                    .asText()
                    .trim();
        }

        /*
         * Fallback: inspect output[].content[].text.
         */
        JsonNode output =
                response.get("output");

        if (
                output != null
                        && output.isArray()
        ) {

            for (JsonNode outputItem : output) {

                JsonNode content =
                        outputItem.get("content");

                if (
                        content == null
                                || !content.isArray()
                ) {

                    continue;
                }

                for (JsonNode contentItem : content) {

                    JsonNode text =
                            contentItem.get("text");

                    if (
                            text != null
                                    && text.isTextual()
                                    && !text.asText().isBlank()
                    ) {

                        return text
                                .asText()
                                .trim();
                    }
                }
            }
        }

        throw new AiProcessingException(
                AiErrorType.INVALID_RESPONSE,
                "AI returned an invalid image analysis response."
        );
    }

    private String extractExtension(
            String filename
    ) {

        int lastDot =
                filename.lastIndexOf('.');

        if (
                lastDot > 0
                        && lastDot < filename.length() - 1
        ) {

            String extension =
                    filename.substring(lastDot);

            /*
             * Keep the extension short and safe.
             */
            if (
                    extension.length() <= 10
                            && extension.matches(
                            "\\.[a-zA-Z0-9]+"
                    )
            ) {

                return extension;
            }
        }

        return ".webm";
    }

    private void requireApiKey() {

        if (
                apiKey == null
                        || apiKey.isBlank()
        ) {

            throw new AiProcessingException(
                    AiErrorType.TEMPORARY,
                    "OpenAI API key is not configured."
            );
        }
    }

    private AiProcessingException classifyHttpException(
            RestClientResponseException exception
    ) {

        HttpStatusCode status =
                exception.getStatusCode();

        int statusCode =
                status.value();

        String responseBody =
                exception.getResponseBodyAsString();

        if (statusCode == 401) {

            return new AiProcessingException(
                    AiErrorType.TEMPORARY,
                    "OpenAI authentication failed. Check the API key configuration.",
                    exception
            );
        }

        if (statusCode == 403) {

            return new AiProcessingException(
                    AiErrorType.UNSUPPORTED,
                    "OpenAI rejected the request because the account or model is not permitted.",
                    exception
            );
        }

        if (statusCode == 429) {

            return new AiProcessingException(
                    AiErrorType.RATE_LIMITED,
                    "AI service is busy or rate limited. Please try again later.",
                    exception
            );
        }

        if (
                statusCode >= 400
                        && statusCode < 500
        ) {

            String detail =
                    responseBody == null
                            || responseBody.isBlank()
                            ? "OpenAI rejected the request."
                            : "OpenAI rejected the request: "
                            + sanitizeError(responseBody);

            return new AiProcessingException(
                    AiErrorType.UNSUPPORTED,
                    detail,
                    exception
            );
        }

        return new AiProcessingException(
                AiErrorType.TEMPORARY,
                "OpenAI is temporarily unavailable. Please try again.",
                exception
        );
    }

    private AiProcessingException classifyException(
            RuntimeException exception
    ) {

        String message =
                exception.getMessage();

        String normalizedMessage =
                message == null
                        ? ""
                        : message.toLowerCase();

        if (
                normalizedMessage.contains("429")
        ) {

            return new AiProcessingException(
                    AiErrorType.RATE_LIMITED,
                    "AI service is busy. Please try again later.",
                    exception
            );
        }

        if (
                normalizedMessage.contains("timeout")
                        || normalizedMessage.contains("timed out")
                        || normalizedMessage.contains("connection")
        ) {

            return new AiProcessingException(
                    AiErrorType.TEMPORARY,
                    "AI service is temporarily unavailable. Please try again.",
                    exception
            );
        }

        return new AiProcessingException(
                AiErrorType.TEMPORARY,
                "AI media processing failed. Please try again later.",
                exception
        );
    }

    private void logImageAiFailure(
            Throwable exception
    ) {

        System.err.println();

        System.err.println(
                "========== IMAGE AI FAILURE =========="
        );

        System.err.println(
                "Exception type: "
                        + exception.getClass().getName()
        );

        System.err.println(
                "Exception message: "
                        + exception.getMessage()
        );

        Throwable cause =
                exception.getCause();

        if (cause != null) {

            System.err.println(
                    "Cause type: "
                            + cause.getClass().getName()
            );

            System.err.println(
                    "Cause message: "
                            + cause.getMessage()
            );
        }

        exception.printStackTrace();

        System.err.println(
                "======================================"
        );

        System.err.println();
    }

    private void logVoiceTranscriptionFailure(
            Throwable exception
    ) {

        System.err.println();

        System.err.println(
                "========== VOICE TRANSCRIPTION FAILURE =========="
        );

        System.err.println(
                "Exception type: "
                        + exception.getClass().getName()
        );

        System.err.println(
                "Exception message: "
                        + exception.getMessage()
        );

        Throwable cause =
                exception.getCause();

        if (cause != null) {

            System.err.println(
                    "Cause type: "
                            + cause.getClass().getName()
            );

            System.err.println(
                    "Cause message: "
                            + cause.getMessage()
            );
        }

        exception.printStackTrace();

        System.err.println(
                "=================================================="
        );

        System.err.println();
    }

    private String sanitizeError(
            String responseBody
    ) {

        String sanitized =
                responseBody
                        .replace("\n", " ")
                        .replace("\r", " ")
                        .trim();

        if (sanitized.length() > 500) {

            return sanitized.substring(0, 500);
        }

        return sanitized;
    }
}