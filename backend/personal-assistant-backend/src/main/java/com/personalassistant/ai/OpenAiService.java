package com.personalassistant.ai;

import com.openai.client.OpenAIClient;
import com.openai.client.okhttp.OpenAIOkHttpClient;
import com.openai.models.ChatModel;
import com.openai.models.responses.ResponseCreateParams;
import com.openai.models.responses.StructuredResponseCreateParams;
import com.personalassistant.dto.AiCaptureAnalysis;
import com.personalassistant.dto.AiPlanData;
import com.personalassistant.exception.AiErrorType;
import com.personalassistant.exception.AiProcessingException;
import org.springframework.stereotype.Service;
import com.personalassistant.dto.ExecutionAnalysis;

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
                                    taskDescription != null
                                            ? taskDescription
                                            : "No description provided.",
                                    estimatedMinutes != null
                                            ? estimatedMinutes
                                            : "Not specified",
                                    actualMinutes,
                                    feedback != null && !feedback.isBlank()
                                            ? feedback
                                            : "No feedback provided."
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