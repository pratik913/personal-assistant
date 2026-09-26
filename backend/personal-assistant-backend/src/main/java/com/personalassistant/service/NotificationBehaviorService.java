package com.personalassistant.service;

import com.personalassistant.dto.NotificationBehaviorInsightResponse;
import com.personalassistant.entity.Notification;
import com.personalassistant.entity.NotificationStatus;
import com.personalassistant.entity.NotificationType;
import com.personalassistant.entity.TaskExecution;
import com.personalassistant.repository.NotificationRepository;
import com.personalassistant.repository.TaskExecutionRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.Instant;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class NotificationBehaviorService {

    private static final long ACTION_WINDOW_MINUTES = 60;

    private final NotificationRepository notificationRepository;
    private final TaskExecutionRepository taskExecutionRepository;

    public NotificationBehaviorInsightResponse getInsights(UUID userId) {

        List<Notification> reminders =
                notificationRepository.findByUserIdAndTypeOrderByScheduledAtDesc(
                        userId,
                        NotificationType.TASK_STARTING
                );

        List<TaskExecution> executions =
                taskExecutionRepository.findByUserIdOrderByStartedAtDesc(userId);

        long totalReminders = reminders.size();

        long readReminders = reminders.stream()
                .filter(notification -> notification.getStatus() == NotificationStatus.READ)
                .count();

        long ignoredReminders = totalReminders - readReminders;

        Map<UUID, List<TaskExecution>> executionsByTask =
                executions.stream()
                        .collect(
                                java.util.stream.Collectors.groupingBy(
                                        execution -> execution.getTask().getId()
                                )
                        );

        long actedOnReminders = 0;

        long totalMinutesToStart = 0;

        long measuredStarts = 0;

        for (Notification notification : reminders) {

            List<TaskExecution> taskExecutions =
                    executionsByTask.getOrDefault(
                            notification.getTask().getId(),
                            List.of()
                    );

            TaskExecution matchingExecution =
                    findMatchingExecution(
                            notification,
                            taskExecutions
                    );

            if (matchingExecution != null) {

                actedOnReminders++;

                long minutes =
                        Duration.between(
                                notification.getScheduledAt(),
                                matchingExecution.getStartedAt()
                        ).toMinutes();

                if (minutes >= 0) {
                    totalMinutesToStart += minutes;
                    measuredStarts++;
                }
            }
        }

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

        double averageMinutesToStart =
                measuredStarts == 0
                        ? 0
                        : (double) totalMinutesToStart / measuredStarts;

        String insight =
                generateInsight(
                        totalReminders,
                        readRate,
                        actionRate,
                        averageMinutesToStart
                );

        return new NotificationBehaviorInsightResponse(
                totalReminders,
                readReminders,
                ignoredReminders,
                actedOnReminders,
                readRate,
                actionRate,
                averageMinutesToStart,
                insight
        );
    }

    private TaskExecution findMatchingExecution(
            Notification notification,
            List<TaskExecution> executions
    ) {

        Instant reminderTime = notification.getScheduledAt();

        Instant actionWindowEnd =
                reminderTime.plus(
                        Duration.ofMinutes(ACTION_WINDOW_MINUTES)
                );

        return executions.stream()
                .filter(execution ->
                        execution.getStartedAt() != null
                )
                .filter(execution ->
                        !execution.getStartedAt().isBefore(reminderTime)
                )
                .filter(execution ->
                        !execution.getStartedAt().isAfter(actionWindowEnd)
                )
                .min(
                        java.util.Comparator.comparing(
                                TaskExecution::getStartedAt
                        )
                )
                .orElse(null);
    }

    private double calculatePercentage(
            long numerator,
            long denominator
    ) {

        if (denominator == 0) {
            return 0;
        }

        return Math.round(
                ((double) numerator / denominator) * 10000
        ) / 100.0;
    }

    private String generateInsight(
            long totalReminders,
            double readRate,
            double actionRate,
            double averageMinutesToStart
    ) {

        if (totalReminders == 0) {
            return "Not enough notification history yet.";
        }

        if (actionRate >= 70) {

            if (averageMinutesToStart > 0) {
                return String.format(
                        "Your reminders are frequently followed by task execution. " +
                                "Average time to start is %.1f minutes.",
                        averageMinutesToStart
                );
            }

            return "Your reminders are frequently followed by task execution.";
        }

        if (actionRate >= 40) {

            return "Some reminders lead to task execution, but there is room to improve reminder timing.";
        }

        if (readRate >= 70) {

            return "You usually read reminders, but task execution often does not follow.";
        }

        return "Many reminders are not being acted on. MindMate can use this behavior to improve future reminder timing.";
    }
}