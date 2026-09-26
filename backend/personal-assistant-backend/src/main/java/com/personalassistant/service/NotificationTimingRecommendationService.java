package com.personalassistant.service;

import com.personalassistant.dto.NotificationTimingRecommendationResponse;
import com.personalassistant.dto.NotificationTimingRecommendationResponse.RecommendationConfidence;
import com.personalassistant.entity.NotificationPreference;
import com.personalassistant.entity.TaskExecution;
import com.personalassistant.entity.TaskExecutionStatus;
import com.personalassistant.repository.NotificationPreferenceRepository;
import com.personalassistant.repository.TaskExecutionRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

@Service
@Transactional(readOnly = true)
public class NotificationTimingRecommendationService {

    private static final int DEFAULT_REMINDER_MINUTES = 15;

    private static final int MIN_REMINDER_MINUTES = 5;

    private static final int MAX_REMINDER_MINUTES = 120;

    private static final int LOW_CONFIDENCE_EXECUTIONS = 1;

    private static final int MEDIUM_CONFIDENCE_EXECUTIONS = 3;

    private static final int HIGH_CONFIDENCE_EXECUTIONS = 6;

    private final NotificationPreferenceRepository notificationPreferenceRepository;

    private final TaskExecutionRepository taskExecutionRepository;

    public NotificationTimingRecommendationService(
            NotificationPreferenceRepository notificationPreferenceRepository,
            TaskExecutionRepository taskExecutionRepository
    ) {
        this.notificationPreferenceRepository =
                notificationPreferenceRepository;

        this.taskExecutionRepository =
                taskExecutionRepository;
    }

    public NotificationTimingRecommendationResponse getRecommendation(
            UUID userId
    ) {

        NotificationPreference preference =
                notificationPreferenceRepository
                        .findByUserId(userId)
                        .orElse(null);

        int currentReminderMinutes =
                preference != null && preference.getReminderMinutes() != null
                        ? preference.getReminderMinutes()
                        : DEFAULT_REMINDER_MINUTES;

        List<TaskExecution> executions =
                taskExecutionRepository
                        .findByUserIdAndStatus(
                                userId,
                                TaskExecutionStatus.COMPLETED
                        );

        List<Long> responseTimes =
                executions.stream()
                        .filter(this::hasValidTiming)
                        .map(this::calculateMinutesToStart)
                        .filter(minutes -> minutes >= 0)
                        .toList();

        long executionCount = responseTimes.size();

        if (executionCount == 0) {

            return new NotificationTimingRecommendationResponse(
                    currentReminderMinutes,
                    currentReminderMinutes,
                    0,
                    0.0,
                    RecommendationConfidence.NONE,
                    "MindMate needs more task execution history before it can recommend a better reminder time."
            );
        }

        double averageMinutesToStart =
                responseTimes.stream()
                        .mapToLong(Long::longValue)
                        .average()
                        .orElse(0.0);

        RecommendationConfidence confidence =
                determineConfidence(executionCount);

        int recommendedReminderMinutes =
                calculateRecommendedReminder(
                        currentReminderMinutes,
                        averageMinutesToStart,
                        confidence
                );

        String reason =
                buildReason(
                        averageMinutesToStart,
                        currentReminderMinutes,
                        recommendedReminderMinutes,
                        confidence
                );

        return new NotificationTimingRecommendationResponse(
                currentReminderMinutes,
                recommendedReminderMinutes,
                executionCount,
                roundToOneDecimal(averageMinutesToStart),
                confidence,
                reason
        );
    }

    private boolean hasValidTiming(TaskExecution execution) {

        return execution.getStartedAt() != null;
    }

    private long calculateMinutesToStart(TaskExecution execution) {

        /*
         * Day 25 V1:
         *
         * We use the execution's createdAt as the closest available
         * historical action timestamp.
         *
         * A future improvement can persist the exact notification
         * that triggered the execution and calculate:
         *
         * notification.scheduledAt -> execution.startedAt
         */
        if (execution.getCreatedAt() == null) {
            return -1;
        }

        Duration duration =
                Duration.between(
                        execution.getCreatedAt(),
                        execution.getStartedAt()
                );

        return duration.toMinutes();
    }

    private RecommendationConfidence determineConfidence(
            long executionCount
    ) {

        if (executionCount >= HIGH_CONFIDENCE_EXECUTIONS) {
            return RecommendationConfidence.HIGH;
        }

        if (executionCount >= MEDIUM_CONFIDENCE_EXECUTIONS) {
            return RecommendationConfidence.MEDIUM;
        }

        if (executionCount >= LOW_CONFIDENCE_EXECUTIONS) {
            return RecommendationConfidence.LOW;
        }

        return RecommendationConfidence.NONE;
    }

    private int calculateRecommendedReminder(
            int currentReminderMinutes,
            double averageMinutesToStart,
            RecommendationConfidence confidence
    ) {

        if (confidence == RecommendationConfidence.NONE) {
            return currentReminderMinutes;
        }

        /*
         * We want the reminder to arrive before the user's
         * observed action time.
         *
         * V1 uses a deterministic buffer rather than AI/ML.
         */
        int recommended;

        if (averageMinutesToStart <= 5) {

            recommended = 10;

        } else if (averageMinutesToStart <= 15) {

            recommended = 15;

        } else if (averageMinutesToStart <= 30) {

            recommended = 30;

        } else if (averageMinutesToStart <= 60) {

            recommended = 45;

        } else {

            recommended = 60;
        }

        /*
         * Do not make low-confidence recommendations
         * dramatically different from the user's preference.
         */
        if (confidence == RecommendationConfidence.LOW) {

            int difference =
                    Math.abs(
                            recommended - currentReminderMinutes
                    );

            if (difference > 15) {

                if (recommended > currentReminderMinutes) {
                    recommended =
                            currentReminderMinutes + 15;
                } else {
                    recommended =
                            currentReminderMinutes - 15;
                }
            }
        }

        return Math.max(
                MIN_REMINDER_MINUTES,
                Math.min(
                        MAX_REMINDER_MINUTES,
                        recommended
                )
        );
    }

    private String buildReason(
            double averageMinutesToStart,
            int currentReminderMinutes,
            int recommendedReminderMinutes,
            RecommendationConfidence confidence
    ) {

        if (recommendedReminderMinutes == currentReminderMinutes) {

            return String.format(
                    "Your current %d-minute reminder timing is consistent with your observed task-start behavior.",
                    currentReminderMinutes
            );
        }

        if (averageMinutesToStart <= 10) {

            return String.format(
                    "You usually start tasks about %.1f minutes after acting on a reminder. A shorter reminder window may be sufficient.",
                    averageMinutesToStart
            );
        }

        if (averageMinutesToStart <= 30) {

            return String.format(
                    "You usually start tasks about %.1f minutes after the reminder interaction. MindMate recommends %d minutes.",
                    averageMinutesToStart,
                    recommendedReminderMinutes
            );
        }

        return String.format(
                "You typically take about %.1f minutes to start tasks. A longer reminder window may give you more preparation time.",
                averageMinutesToStart
        );
    }

    private double roundToOneDecimal(double value) {

        return Math.round(value * 10.0) / 10.0;
    }
}