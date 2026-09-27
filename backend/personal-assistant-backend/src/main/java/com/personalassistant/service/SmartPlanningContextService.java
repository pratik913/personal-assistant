package com.personalassistant.service;

import com.personalassistant.dto.CreateAiPlanRequest;
import com.personalassistant.entity.ScheduleEntry;
import com.personalassistant.entity.Task;
import com.personalassistant.entity.TaskExecution;
import com.personalassistant.entity.TaskExecutionStatus;
import com.personalassistant.repository.TaskExecutionRepository;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.Instant;
import java.time.ZoneId;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.UUID;

/**
 * Builds deterministic planning intelligence that is supplied
 * to the AI planner as structured context.
 *
 * Important architectural rule:
 *
 * AI does not calculate authoritative business metrics.
 * The application calculates them and gives the AI the facts.
 */
@Service
public class SmartPlanningContextService {

    /**
     * Execution sessions longer than this are considered
     * unusually long for planning purposes.
     */
    private static final long MAX_PLANNING_SESSION_MINUTES = 8 * 60;

    /**
     * Percentage of usable capacity reserved for unexpected
     * interruptions, transitions and small delays.
     */
    private static final double BUFFER_RATIO = 0.10;

    /**
     * Minimum buffer so even a small planning window
     * gets some protection.
     */
    private static final int MIN_BUFFER_MINUTES = 10;

    /**
     * Maximum buffer so large planning windows don't
     * become excessively conservative.
     */
    private static final int MAX_BUFFER_MINUTES = 60;

    private final TaskExecutionRepository taskExecutionRepository;

    public SmartPlanningContextService(
            TaskExecutionRepository taskExecutionRepository
    ) {
        this.taskExecutionRepository =
                taskExecutionRepository;
    }

    /**
     * Builds the planning intelligence section that will be
     * appended to the normal AI planning context.
     */
    public String buildPlanningIntelligence(
            UUID userId,
            CreateAiPlanRequest request,
            List<Task> tasks,
            List<ScheduleEntry> scheduleEntries,
            ZoneId zoneId
    ) {

        int availableMinutes =
                calculateAvailableMinutes(request);

        int scheduledMinutes =
                calculateExistingScheduleMinutes(
                        scheduleEntries
                );

        int rawFreeMinutes =
                Math.max(
                        0,
                        availableMinutes - scheduledMinutes
                );

        int bufferMinutes =
                calculateBufferMinutes(
                        rawFreeMinutes
                );

        int usableCapacityMinutes =
                Math.max(
                        0,
                        rawFreeMinutes - bufferMinutes
                );

        StringBuilder context =
                new StringBuilder();

        context.append(
                "SMART PLANNING INTELLIGENCE:\n"
        );

        context.append(
                "Planning capacity:\n"
        );

        context.append(
                        "- Requested availability: "
                )
                .append(
                        request.getAvailableFrom()
                )
                .append(" - ")
                .append(
                        request.getAvailableUntil()
                )
                .append("\n");

        context.append(
                        "- Available minutes: "
                )
                .append(
                        availableMinutes
                )
                .append("\n");

        context.append(
                        "- Existing scheduled minutes: "
                )
                .append(
                        scheduledMinutes
                )
                .append("\n");

        context.append(
                        "- Free minutes before buffer: "
                )
                .append(
                        rawFreeMinutes
                )
                .append("\n");

        context.append(
                        "- Planning buffer: "
                )
                .append(
                        bufferMinutes
                )
                .append("\n");

        context.append(
                        "- Usable planning capacity: "
                )
                .append(
                        usableCapacityMinutes
                )
                .append("\n\n");

        context.append(
                "Task duration intelligence:\n"
        );

        if (tasks.isEmpty()) {

            context.append(
                    "- No pending tasks are available.\n"
            );

        } else {

            for (Task task : tasks) {

                TaskIntelligence intelligence =
                        calculateTaskIntelligence(
                                task,
                                userId
                        );

                context.append(
                                "- Task ID: "
                        )
                        .append(
                                task.getId()
                        )
                        .append("\n");

                context.append(
                                "  Title: "
                        )
                        .append(
                                task.getTitle()
                        )
                        .append("\n");

                context.append(
                                "  Priority: "
                        )
                        .append(
                                task.getPriority()
                        )
                        .append("\n");

                context.append(
                                "  Current estimate: "
                        )
                        .append(
                                nullableMinutes(
                                        task.getEstimatedMinutes()
                                )
                        )
                        .append("\n");

                context.append(
                                "  Historical average: "
                        )
                        .append(
                                intelligence.averageMinutes()
                        )
                        .append("\n");

                context.append(
                                "  Recommended planning duration: "
                        )
                        .append(
                                intelligence.recommendedMinutes()
                        )
                        .append("\n");

                context.append(
                                "  Execution history used: "
                        )
                        .append(
                                intelligence.executionCount()
                        )
                        .append("\n");

                context.append(
                                "  Planning confidence: "
                        )
                        .append(
                                intelligence.confidence()
                        )
                        .append("\n");

                if (task.getDueDate() != null) {

                    context.append(
                                    "  Due date: "
                            )
                            .append(
                                    task.getDueDate()
                            )
                            .append("\n");
                }

                context.append("\n");
            }
        }

        context.append(
                "PLANNING RULES:\n"
        );

        context.append(
                "- Prefer recommended planning duration when confidence is LOW, MEDIUM or HIGH.\n"
        );

        context.append(
                "- For confidence NONE, use the current estimate when available.\n"
        );

        context.append(
                "- Do not schedule more work than the usable planning capacity unless absolutely necessary.\n"
        );

        context.append(
                "- Respect existing schedule entries.\n"
        );

        context.append(
                "- Prefer higher-priority tasks when capacity is limited.\n"
        );

        context.append(
                "- Prefer tasks with earlier due dates when priority is otherwise similar.\n"
        );

        context.append(
                "- Leave the planning buffer unused rather than filling every available minute.\n"
        );

        context.append(
                "- Never invent task IDs.\n"
        );

        context.append(
                "- Never schedule outside the requested availability window.\n"
        );

        return context.toString();
    }

    /**
     * Calculates the total requested planning window.
     */
    private int calculateAvailableMinutes(
            CreateAiPlanRequest request
    ) {

        long minutes =
                Duration.between(
                        request.getAvailableFrom(),
                        request.getAvailableUntil()
                ).toMinutes();

        if (minutes <= 0) {
            throw new IllegalArgumentException(
                    "Available time window must be positive."
            );
        }

        return Math.toIntExact(
                minutes
        );
    }

    /**
     * Calculates how much of the planning window is already
     * occupied by existing schedule entries.
     */
    private int calculateExistingScheduleMinutes(
            List<ScheduleEntry> scheduleEntries
    ) {

        if (
                scheduleEntries == null ||
                        scheduleEntries.isEmpty()
        ) {
            return 0;
        }

        return scheduleEntries.stream()
                .filter(entry ->
                        entry.getStartAt() != null &&
                                entry.getEndAt() != null
                )
                .mapToInt(entry ->
                        (int) Math.max(
                                0,
                                Duration.between(
                                        entry.getStartAt(),
                                        entry.getEndAt()
                                ).toMinutes()
                        )
                )
                .sum();
    }

    /**
     * Calculates a bounded planning buffer.
     */
    private int calculateBufferMinutes(
            int freeMinutes
    ) {

        if (freeMinutes <= 0) {
            return 0;
        }

        int calculated =
                (int) Math.ceil(
                        freeMinutes * BUFFER_RATIO
                );

        return Math.min(
                MAX_BUFFER_MINUTES,
                Math.max(
                        MIN_BUFFER_MINUTES,
                        calculated
                )
        );
    }

    /**
     * Calculates historical intelligence for one task.
     */
    private TaskIntelligence calculateTaskIntelligence(
            Task task,
            UUID userId
    ) {

        List<TaskExecution> executions =
                taskExecutionRepository
                        .findByTaskIdAndUserIdAndStatusOrderByStartedAtDesc(
                                task.getId(),
                                userId,
                                TaskExecutionStatus.COMPLETED
                        );

        List<Long> eligibleDurations =
                executions.stream()
                        .filter(execution ->
                                execution.getStartedAt() != null &&
                                        execution.getEndedAt() != null
                        )
                        .map(execution ->
                                Duration.between(
                                        execution.getStartedAt(),
                                        execution.getEndedAt()
                                ).toMinutes()
                        )
                        .filter(minutes ->
                                minutes > 0 &&
                                        minutes <=
                                                MAX_PLANNING_SESSION_MINUTES
                        )
                        .toList();

        int executionCount =
                eligibleDurations.size();

        if (executionCount == 0) {

            return new TaskIntelligence(
                    nullableMinutes(
                            task.getEstimatedMinutes()
                    ),
                    nullableMinutes(
                            task.getEstimatedMinutes()
                    ),
                    0,
                    "NONE"
            );
        }

        double average =
                eligibleDurations.stream()
                        .mapToLong(Long::longValue)
                        .average()
                        .orElse(
                                task.getEstimatedMinutes() == null
                                        ? 0
                                        : task.getEstimatedMinutes()
                        );

        int roundedAverage =
                Math.max(
                        1,
                        (int) Math.round(
                                average
                        )
                );

        String confidence =
                determineConfidence(
                        executionCount
                );

        int recommended =
                calculateRecommendedDuration(
                        task.getEstimatedMinutes(),
                        roundedAverage,
                        confidence
                );

        return new TaskIntelligence(
                String.valueOf(roundedAverage),
                String.valueOf(recommended),
                executionCount,
                confidence
        );
    }

    /**
     * Uses the same confidence concept as adaptive planning:
     *
     * 0       -> NONE
     * 1       -> LOW
     * 2-4     -> MEDIUM
     * 5+      -> HIGH
     */
    private String determineConfidence(
            int executionCount
    ) {

        if (executionCount <= 0) {
            return "NONE";
        }

        if (executionCount == 1) {
            return "LOW";
        }

        if (executionCount <= 4) {
            return "MEDIUM";
        }

        return "HIGH";
    }

    /**
     * Blends the user's current estimate with historical
     * behavior depending on confidence.
     */
    private int calculateRecommendedDuration(
            Integer currentEstimate,
            int historicalAverage,
            String confidence
    ) {

        if (currentEstimate == null) {
            return historicalAverage;
        }

        if ("LOW".equals(confidence)) {

            return weightedAverage(
                    currentEstimate,
                    historicalAverage,
                    0.75,
                    0.25
            );
        }

        if ("MEDIUM".equals(confidence)) {

            return weightedAverage(
                    currentEstimate,
                    historicalAverage,
                    0.40,
                    0.60
            );
        }

        if ("HIGH".equals(confidence)) {

            return historicalAverage;
        }

        return currentEstimate;
    }

    private int weightedAverage(
            int current,
            int historical,
            double currentWeight,
            double historicalWeight
    ) {

        return Math.max(
                1,
                (int) Math.round(
                        current * currentWeight +
                                historical * historicalWeight
                )
        );
    }

    private String nullableMinutes(
            Integer minutes
    ) {

        return minutes == null
                ? "NOT_SET"
                : minutes.toString();
    }

    private record TaskIntelligence(
            String averageMinutes,
            String recommendedMinutes,
            int executionCount,
            String confidence
    ) {
    }
}