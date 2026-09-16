package com.personalassistant.ai;

import com.openai.client.OpenAIClient;
import com.openai.client.okhttp.OpenAIOkHttpClient;
import com.openai.models.ChatModel;
import com.openai.models.responses.ResponseCreateParams;
import com.openai.models.responses.StructuredResponseCreateParams;
import com.personalassistant.dto.AiCaptureAnalysis;
import org.springframework.stereotype.Service;

@Service
public class OpenAiService implements AiService {

    private final OpenAIClient client;
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
        - If the capture does not represent an actionable intention,
          return an empty task list.
        - Return only the requested structured output.
        """;
    public OpenAiService() {
        this.client = OpenAIOkHttpClient.fromEnv();
    }

    @Override
    public AiCaptureAnalysis analyzeCapture(String content) {

        StructuredResponseCreateParams<AiCaptureAnalysis> params =
                ResponseCreateParams.builder()
                        .input("""
                            %s

                            User capture:
                            %s
                            """.formatted(SYSTEM_INSTRUCTION, content))
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
                        new IllegalStateException(
                                "AI returned no structured output"
                        )
                );
    }
}