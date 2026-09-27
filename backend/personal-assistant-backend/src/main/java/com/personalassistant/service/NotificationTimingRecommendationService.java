package com.personalassistant.service;

import com.personalassistant.dto.NotificationTimingRecommendationResponse;
import com.personalassistant.dto.NotificationTimingRecommendationResponse.RecommendationConfidence;
import com.personalassistant.entity.Notification;
import com.personalassistant.entity.NotificationPreference;
import com.personalassistant.entity.TaskExecution;
import com.personalassistant.entity.TaskExecutionStatus;
import com.personalassistant.repository.NotificationPreferenceRepository;
import com.personalassistant.repository.TaskExecutionRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.util.List;
import java.util.UUID;

@Service
@Transactional(readOnly = true)
public class NotificationTimingRecommendationService {

    private static final int DEFAULT_REMINDER_MINUTES = 15;

    private static final int LOW_CONFIDENCE_EXECUTIONS = 1;

    private static final int MEDIUM_CONFIDENCE_EXECUTIONS = 3;

    private static final int HIGH_CONFIDENCE_EXECUTIONS = 6;

    /*
     * Day 25 recommendation ceiling.
     */
    private static final int MAX_RECOMMENDED_REMINDER_MINUTES = 120;


    private final NotificationPreferenceRepository
            notificationPreferenceRepository;

    private final TaskExecutionRepository
            taskExecutionRepository;


    public NotificationTimingRecommendationService(
            NotificationPreferenceRepository
                    notificationPreferenceRepository,
            TaskExecutionRepository
                    taskExecutionRepository
    ) {

        this.notificationPreferenceRepository =
                notificationPreferenceRepository;

        this.taskExecutionRepository =
                taskExecutionRepository;

    }


    public NotificationTimingRecommendationResponse
    getRecommendation(
            UUID userId
    ) {

        NotificationPreference preference =
                notificationPreferenceRepository
                        .findByUserId(userId)
                        .orElse(null);


        int currentReminderMinutes =
                preference != null &&
                        preference.getReminderMinutes() != null

                        ? preference.getReminderMinutes()

                        : DEFAULT_REMINDER_MINUTES;


        /*
         * =================================================
         * ONLY CORRELATED COMPLETED EXECUTIONS
         * =================================================
         */

        List<TaskExecution> executions =
                taskExecutionRepository
                        .findByUserIdAndStatus(
                                userId,
                                TaskExecutionStatus.COMPLETED
                        )
                        .stream()
                        .filter(execution ->
                                execution.getNotification()
                                        != null
                        )
                        .toList();


        List<Long> responseTimes =
                executions
                        .stream()
                        .map(this::calculateResponseMinutes)
                        .filter(minutes ->
                                minutes >= 0
                        )
                        .toList();


        long executionCount =
                responseTimes.size();


        if (
                executionCount == 0
        ) {

            return new NotificationTimingRecommendationResponse(

                    currentReminderMinutes,

                    currentReminderMinutes,

                    0,

                    0.0,

                    RecommendationConfidence.NONE,

                    "MindMate needs more notification-to-execution history before it can recommend a better reminder time."

            );

        }


        double averageMinutesToStart =
                responseTimes
                        .stream()
                        .mapToLong(Long::longValue)
                        .average()
                        .orElse(0.0);


        RecommendationConfidence confidence =
                determineConfidence(
                        executionCount
                );


        int recommendedReminderMinutes =
                calculateRecommendedReminder(
                        averageMinutesToStart,
                        confidence
                );


        String reason =
                buildReason(
                        averageMinutesToStart,
                        confidence
                );


        return new NotificationTimingRecommendationResponse(

                currentReminderMinutes,

                recommendedReminderMinutes,

                executionCount,

                round(
                        averageMinutesToStart
                ),

                confidence,

                reason

        );

    }


    private long calculateResponseMinutes(
            TaskExecution execution
    ) {

        Notification notification =
                execution.getNotification();


        if (
                notification == null ||
                        notification.getScheduledAt() == null ||
                        execution.getStartedAt() == null
        ) {

            return -1;

        }


        Duration responseTime =
                Duration.between(
                        notification.getScheduledAt(),
                        execution.getStartedAt()
                );


        if (
                responseTime.isNegative()
        ) {

            return -1;

        }


        return responseTime.toMinutes();

    }


    private RecommendationConfidence determineConfidence(
            long executionCount
    ) {

        if (
                executionCount >=
                        HIGH_CONFIDENCE_EXECUTIONS
        ) {

            return RecommendationConfidence.HIGH;

        }


        if (
                executionCount >=
                        MEDIUM_CONFIDENCE_EXECUTIONS
        ) {

            return RecommendationConfidence.MEDIUM;

        }


        if (
                executionCount >=
                        LOW_CONFIDENCE_EXECUTIONS
        ) {

            return RecommendationConfidence.LOW;

        }


        return RecommendationConfidence.NONE;

    }


    private int calculateRecommendedReminder(
            double averageMinutesToStart,
            RecommendationConfidence confidence
    ) {

        int recommendation;


        if (
                averageMinutesToStart <= 5
        ) {

            recommendation = 10;

        } else if (
                averageMinutesToStart <= 15
        ) {

            recommendation = 15;

        } else if (
                averageMinutesToStart <= 30
        ) {

            recommendation = 30;

        } else if (
                averageMinutesToStart <= 60
        ) {

            recommendation = 45;

        } else if (
                averageMinutesToStart <= 120
        ) {

            recommendation = 60;

        } else {

            recommendation = 120;

        }


        /*
         * With very little history, avoid making a
         * large change based on a single execution.
         */
        if (
                confidence == RecommendationConfidence.LOW
        ) {

            return recommendation;

        }


        return Math.min(
                recommendation,
                MAX_RECOMMENDED_REMINDER_MINUTES
        );

    }


    private String buildReason(
            double averageMinutesToStart,
            RecommendationConfidence confidence
    ) {

        if (
                averageMinutesToStart <= 5
        ) {

            return "You usually start tasks shortly after receiving reminders. A shorter reminder window may be sufficient.";

        }


        if (
                averageMinutesToStart <= 15
        ) {

            return "You usually start tasks fairly soon after receiving reminders. MindMate can use this pattern to refine future reminder timing.";

        }


        if (
                averageMinutesToStart <= 30
        ) {

            return "You tend to start tasks within about half an hour of reminders. MindMate can use this pattern for future reminder timing.";

        }


        return "Your task executions usually happen later after reminders. A longer reminder window may give you more useful preparation time.";

    }


    private double round(
            double value
    ) {

        return Math.round(
                value * 100.0
        ) / 100.0;

    }

}