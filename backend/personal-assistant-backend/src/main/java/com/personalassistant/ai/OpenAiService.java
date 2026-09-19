package com.personalassistant.ai;

import com.openai.client.OpenAIClient;
import com.openai.client.okhttp.OpenAIOkHttpClient;
import com.openai.models.ChatModel;
import com.openai.models.responses.ResponseCreateParams;
import com.openai.models.responses.StructuredResponseCreateParams;
import com.personalassistant.dto.AiCaptureAnalysis;
import com.personalassistant.exception.AiErrorType;
import com.personalassistant.exception.AiProcessingException;
import org.springframework.stereotype.Service;

@Service
public class OpenAiService implements AiService {

    private static final String SYSTEM_INSTRUCTION = """
        You are an AI productivity assistant.

        Your job is to analyze a user's captured content and identify
        the user's actual intention.

        Convert that intention into useful, actionable tasks.

        Rules:
        - Only create tasks that are directly implied by the user's capture.
        - Do not invent unrelated tasks.
        - Do not automatically create preparation, administrative,
          scheduling, or follow-up tasks unless they are explicitly
          requested or clearly implied.
        - Prefer fewer meaningful tasks over many small tasks.
        - If the capture represents one simple intention, return one task.
        - Create multiple tasks only when the capture clearly contains
          multiple distinct actionable objectives.
        - Keep task titles concise and actionable.
        - Provide a useful description for each task.
        - Estimate a realistic amount of time in minutes.
        - Assign a priority to every task.

        Priority rules:
        - HIGH: Important or urgent tasks, tasks with explicit deadlines,
          or tasks where delaying completion could have a significant
          negative impact.
        - MEDIUM: Normal meaningful work that should be completed but
          is not urgent or time-sensitive.
        - LOW: Optional, exploratory, nice-to-have, or low-impact tasks.

        Do not assign HIGH priority merely because a task sounds useful.
        Use the context of the user's capture when deciding priority.

        If the capture does not represent an actionable intention,
        return an empty task list.

        Return only the requested structured output.
        """;

    private final OpenAIClient client;

    public OpenAiService() {
        this.client = OpenAIOkHttpClient.fromEnv();
    }

    @Override
    public AiCaptureAnalysis analyzeCapture(String content) {

        try {

            StructuredResponseCreateParams<AiCaptureAnalysis> params =
                    ResponseCreateParams.builder()
                            .input("""
                                %s

                                User capture:
                                %s
                                """.formatted(
                                    SYSTEM_INSTRUCTION,
                                    content
                            ))
                            .model(ChatModel.GPT_5)
                            .text(AiCaptureAnalysis.class)
                            .build();

            return client.responses()
                    .create(params)
                    .output()
                    .stream()
                    .flatMap(item -> item.message().stream())
                    .flatMap(message -> message.content().stream())
                    .flatMap(contentItem -> contentItem.outputText().stream())
                    .findFirst()
                    .orElseThrow(() ->
                            new AiProcessingException(
                                    AiErrorType.INVALID_RESPONSE,
                                    "AI returned an invalid response."
                            )
                    );

        } catch (AiProcessingException exception) {

            throw exception;

        } catch (RuntimeException exception) {

            throw classifyException(exception);
        }
    }

    private AiProcessingException classifyException(
            RuntimeException exception
    ) {

        String message = exception.getMessage();

        if (message != null &&
                message.contains("429")) {

            return new AiProcessingException(
                    AiErrorType.RATE_LIMITED,
                    "AI service is busy. Please try again later.",
                    exception
            );
        }

        if (message != null &&
                (message.contains("timeout")
                        || message.contains("timed out")
                        || message.contains("connection"))) {

            return new AiProcessingException(
                    AiErrorType.TEMPORARY,
                    "AI service is temporarily unavailable. Please try again.",
                    exception
            );
        }

        return new AiProcessingException(
                AiErrorType.TEMPORARY,
                "AI processing failed. Please try again later.",
                exception
        );
    }
}