package com.personalassistant.ai;

import com.openai.client.OpenAIClient;
import com.openai.client.okhttp.OpenAIOkHttpClient;
import com.openai.models.ChatModel;
import com.openai.models.responses.ResponseCreateParams;
import com.openai.models.responses.StructuredResponseCreateParams;
import com.personalassistant.dto.AiCaptureAnalysis;
import com.personalassistant.dto.AiPlanData;
import com.personalassistant.dto.ExecutionAnalysis;
import com.personalassistant.exception.AiErrorType;
import com.personalassistant.exception.AiProcessingException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.ZoneId;

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

            Due date rules:
            - Set dueDate only when the user's capture contains a clear
              temporal intention for when the task should be completed.
            - If there is no clear due date, return dueDate as null.
            - Resolve relative dates such as "today", "tomorrow",
              "day after tomorrow", "next Monday", etc. using the
              provided current local date and timezone.
            - "Tomorrow morning", "tomorrow evening", etc. should resolve
              to tomorrow's date. Do not invent a specific clock time.
            - The task model stores a date only, not a time of day.
            - Never use the server's timezone when resolving relative dates.
            - Do not create a due date merely because the task is important.
            - Do not infer a deadline when the user did not express one.

            URL / Instagram rules:
            - When the capture is a GENERIC_URL, use the provided URL and
              user context to understand the user's intention.
            - When the capture is an INSTAGRAM_REEL, treat the URL as a
              reference to an Instagram Reel.
            - A bare Instagram URL does not guarantee access to the Reel's
              actual video, caption, audio, or private content.
            - Never invent or claim details about an Instagram Reel that
              were not provided by the user.
            - If the user provides context describing what they want from
              the Reel, use that context to create the actionable task.
            - If the user simply wants to save or revisit the Reel, create
              a task only if the user's wording clearly expresses an
              actionable intention.
            - Do not create unrelated tasks merely because an Instagram URL
              was provided.

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

    private static final String PLANNER_SYSTEM_INSTRUCTION = """
            You are an AI productivity planner.

            Your job is to create a realistic daily plan from the user's
            available tasks and available time.

            Rules:
            - Only schedule tasks provided in the planning context.
            - Never invent tasks.
            - Prefer HIGH priority tasks when there is limited available time.
            - Consider estimated task duration.
            - Do not schedule a task outside the user's available time.
            - Do not overlap planned tasks.
            - Respect existing scheduled tasks.
            - Keep reasonable gaps between tasks when appropriate.
            - Do not schedule more time than the task's estimated duration.
            - If there is not enough time for all tasks, prioritize the most
              important tasks and leave lower-priority tasks unscheduled.
            - Return task IDs exactly as provided.
            - Every planned item must have a clear reason.
            - The planning date and available times are expressed in the
              user's timezone.
            - Convert planned local times to ISO-8601 timestamps using UTC
              offsets when returning startAt and endAt.
            - Do not interpret the provided local times as UTC.
            - Return only the requested structured output.
            """;

    private final OpenAIClient client;

    public OpenAiService(
            @Value("${openai.api-key:}") String apiKey
    ) {

        if (apiKey == null || apiKey.isBlank()) {
            throw new IllegalStateException(
                    "OpenAI API key is not configured."
            );
        }

        this.client = OpenAIOkHttpClient
                .builder()
                .apiKey(apiKey)
                .build();
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

    @Override
    public AiPlanData generatePlan(
            String planningContext
    ) {

        try {

            StructuredResponseCreateParams<AiPlanData> params =
                    ResponseCreateParams.builder()
                            .input("""
                                    %s

                                    Planning context:
                                    %s
                                    """.formatted(
                                    PLANNER_SYSTEM_INSTRUCTION,
                                    planningContext
                            ))
                            .model(ChatModel.GPT_5)
                            .text(AiPlanData.class)
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
                                    "AI returned an invalid planning response."
                            )
                    );

        } catch (AiProcessingException exception) {

            throw exception;

        } catch (RuntimeException exception) {

            throw classifyException(exception);
        }
    }

    @Override
    public ExecutionAnalysis analyzeExecution(
            String taskTitle,
            String taskDescription,
            Integer estimatedMinutes,
            long actualMinutes,
            String feedback
    ) {

        try {

            StructuredResponseCreateParams<ExecutionAnalysis> params =
                    ResponseCreateParams.builder()
                            .input("""
                                    You are an AI productivity assistant.

                                    Your job is to analyze how a user performed a task
                                    after completing a work session.

                                    Analyze the task information, estimated duration,
                                    actual duration, and user's feedback.

                                    Rules:
                                    - Determine the difficulty as EASY, MEDIUM, or HARD.
                                    - Determine whether the original time estimate was
                                      UNDERESTIMATED, ACCURATE, or OVERESTIMATED.
                                    - Identify the main blocker if one exists.
                                    - Use NONE when there was no meaningful blocker.
                                    - Do not invent a blocker that is not supported
                                      by the feedback.
                                    - Keep the insight concise and factual.
                                    - Provide a practical suggestion when useful.
                                    - Do not modify the task.
                                    - Return only the requested structured output.

                                    Task title:
                                    %s

                                    Task description:
                                    %s

                                    Estimated duration:
                                    %s minutes

                                    Actual duration:
                                    %s minutes

                                    User feedback:
                                    %s
                                    """.formatted(
                                    taskTitle,
                                    taskDescription,
                                    estimatedMinutes,
                                    actualMinutes,
                                    feedback
                            ))
                            .model(ChatModel.GPT_5)
                            .text(ExecutionAnalysis.class)
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
                                    "AI returned an invalid execution analysis."
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

        if (message == null) {
            message = "";
        }

        String normalized = message.toLowerCase();

        if (normalized.contains("429")
                || normalized.contains("rate limit")
                || normalized.contains("too many requests")) {

            return new AiProcessingException(
                    AiErrorType.RATE_LIMITED,
                    "AI service rate limit reached.",
                    exception
            );
        }

        if (normalized.contains("timeout")
                || normalized.contains("timed out")
                || normalized.contains("connection")
                || normalized.contains("temporarily")
                || normalized.contains("503")
                || normalized.contains("502")
                || normalized.contains("500")) {

            return new AiProcessingException(
                    AiErrorType.TEMPORARY,
                    "AI service is temporarily unavailable.",
                    exception
            );
        }

        if (normalized.contains("unsupported")
                || normalized.contains("not supported")) {

            return new AiProcessingException(
                    AiErrorType.UNSUPPORTED,
                    "AI service does not support this request.",
                    exception
            );
        }

        return new AiProcessingException(
                AiErrorType.INVALID_RESPONSE,
                "AI processing failed.",
                exception
        );
    }
}