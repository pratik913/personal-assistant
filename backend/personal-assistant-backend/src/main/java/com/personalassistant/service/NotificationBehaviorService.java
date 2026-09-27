package com.personalassistant.service;

import com.personalassistant.dto.NotificationBehaviorInsightResponse;
import com.personalassistant.entity.Notification;
import com.personalassistant.entity.NotificationStatus;
import com.personalassistant.entity.NotificationType;
import com.personalassistant.entity.TaskExecution;
import com.personalassistant.entity.TaskExecutionStatus;
import com.personalassistant.repository.NotificationRepository;
import com.personalassistant.repository.TaskExecutionRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.Instant;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

@Service
@Transactional(readOnly = true)
public class NotificationBehaviorService {

    private final NotificationRepository notificationRepository;

    private final TaskExecutionRepository taskExecutionRepository;


    public NotificationBehaviorService(
            NotificationRepository notificationRepository,
            TaskExecutionRepository taskExecutionRepository
    ) {

        this.notificationRepository =
                notificationRepository;

        this.taskExecutionRepository =
                taskExecutionRepository;

    }


    public NotificationBehaviorInsightResponse getInsights(
            UUID userId
    ) {

        /*
         * =================================================
         * ALL TASK-STARTING REMINDERS
         * =================================================
         */

        List<Notification> notifications =
                notificationRepository
                        .findByUserIdAndTypeOrderByScheduledAtDesc(
                                userId,
                                NotificationType.TASK_STARTING
                        );


        long totalReminders =
                notifications.size();


        long readReminders =
                notifications
                        .stream()
                        .filter(notification ->
                                notification.getStatus() ==
                                        NotificationStatus.READ
                        )
                        .count();


        long ignoredReminders =
                totalReminders -
                        readReminders;


        /*
         * =================================================
         * CORRELATED EXECUTIONS
         * =================================================
         *
         * Day 25 used time-based matching.
         *
         * Day 26 uses the actual notification relationship.
         */

        List<TaskExecution> completedExecutions =
                taskExecutionRepository
                        .findByUserIdAndStatus(
                                userId,
                                TaskExecutionStatus.COMPLETED
                        );


        Set<UUID> actedNotificationIds =
                new HashSet<>();


        List<Long> responseTimes =
                completedExecutions
                        .stream()

                        .filter(execution ->
                                execution.getNotification() != null
                        )

                        .map(execution ->
                                execution
                                        .getNotification()
                        )

                        .filter(notification ->
                                notification.getId() != null
                        )

                        .filter(notification ->
                                actedNotificationIds.add(
                                        notification.getId()
                                )
                        )

                        .map(this::calculateResponseMinutes)
                        .filter(minutes ->
                                minutes >= 0
                        )
                        .toList();


        long actedOnReminders =
                actedNotificationIds.size();


        /*
         * =================================================
         * RATES
         * =================================================
         */

        double readRate =
                calculatePercentage(
                        readReminders,
                        totalReminders
                );


        double actionRate =
                calculatePercentage(
                        actedOnReminders,
                        totalReminders
                );


        /*
         * =================================================
         * AVERAGE RESPONSE TIME
         * =================================================
         */

        double averageMinutesToStart =
                responseTimes
                        .stream()
                        .mapToLong(
                                Long::longValue
                        )
                        .average()
                        .orElse(0.0);


        /*
         * =================================================
         * INSIGHT
         * =================================================
         */

        String insight =
                buildInsight(
                        totalReminders,
                        readReminders,
                        actedOnReminders,
                        averageMinutesToStart
                );


        return new NotificationBehaviorInsightResponse(

                totalReminders,

                readReminders,

                ignoredReminders,

                actedOnReminders,

                readRate,

                actionRate,

                round(
                        averageMinutesToStart
                ),

                insight

        );

    }


    private long calculateResponseMinutes(
            Notification notification
    ) {

        if (
                notification.getScheduledAt() == null
        ) {

            return -1;

        }


        /*
         * Find the execution associated with this
         * notification.
         *
         * There should normally be only one because
         * correlation prevents reuse.
         */
        return taskExecutionRepository
                .findByUserIdOrderByStartedAtDesc(
                        notification.getUser().getId()
                )
                .stream()

                .filter(execution ->
                        execution.getNotification() != null
                )

                .filter(execution ->
                        notification.getId()
                                .equals(
                                        execution
                                                .getNotification()
                                                .getId()
                                )
                )

                .map(TaskExecution::getStartedAt)

                .filter(startedAt ->
                        startedAt != null
                )

                .findFirst()

                .map(startedAt ->
                        Duration
                                .between(
                                        notification.getScheduledAt(),
                                        startedAt
                                )
                                .toMinutes()
                )

                .orElse(-1L);

    }


    private double calculatePercentage(
            long numerator,
            long denominator
    ) {

        if (
                denominator == 0
        ) {

            return 0.0;

        }


        return round(
                (numerator * 100.0) /
                        denominator
        );

    }


    private double round(
            double value
    ) {

        return Math.round(
                value * 100.0
        ) / 100.0;

    }


    private String buildInsight(
            long totalReminders,
            long readReminders,
            long actedOnReminders,
            double averageMinutesToStart
    ) {

        if (
                totalReminders == 0
        ) {

            return "MindMate needs more reminder history before it can learn your notification behavior.";

        }


        if (
                actedOnReminders == 0
        ) {

            if (
                    readReminders == 0
            ) {

                return "Your reminders have not been opened yet. MindMate needs more interaction data to learn your preferred reminder timing.";

            }


            return "Some reminders have been opened, but none are yet linked to a task execution. MindMate needs more execution data to learn your reminder timing.";

        }


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


        return "Your reminder-to-execution timing varies. MindMate can use more execution history to improve future reminder timing.";

    }

}