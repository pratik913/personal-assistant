package com.personalassistant.service;

import com.personalassistant.dto.NotificationDecision;
import com.personalassistant.entity.NotificationPreference;
import com.personalassistant.entity.Task;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.time.LocalTime;

@Service
public class NotificationPolicyService {

    /**
     * Determines whether a task-start notification
     * should be created.
     */
    public NotificationDecision evaluateTaskStartNotification(
            Task task,
            NotificationPreference preference,
            Instant scheduledAt
    ) {

        if (task == null) {

            return NotificationDecision.reject(
                    "Task is required."
            );
        }

        if (preference == null) {

            return NotificationDecision.allow();
        }

        if (
                !preference.isTaskStartNotificationsEnabled()
        ) {

            return NotificationDecision.reject(
                    "Task-start notifications are disabled."
            );
        }

        if (
                isInsideQuietHours(
                        preference,
                        scheduledAt
                )
        ) {

            return NotificationDecision.reject(
                    "Notification falls inside quiet hours."
            );
        }

        return NotificationDecision.allow();
    }

    /**
     * Determines whether the notification time falls
     * inside the user's configured quiet-hours window.
     */
    private boolean isInsideQuietHours(
            NotificationPreference preference,
            Instant scheduledAt
    ) {

        if (
                !preference.isQuietHoursEnabled()
        ) {

            return false;
        }

        if (
                preference.getQuietHoursStart() == null ||
                        preference.getQuietHoursEnd() == null
        ) {

            return false;
        }

        /*
         * Instant is converted to the application's
         * current JVM timezone for V1.
         *
         * User timezone can be incorporated into
         * this policy when notification delivery
         * becomes timezone-aware.
         */
        LocalTime notificationTime =
                scheduledAt
                        .atZone(
                                java.time.ZoneId.systemDefault()
                        )
                        .toLocalTime();

        LocalTime start =
                preference.getQuietHoursStart();

        LocalTime end =
                preference.getQuietHoursEnd();

        /*
         * Normal window:
         *
         * 22:00 -> 23:59
         * 00:00 -> 08:00
         *
         * is represented by a window crossing midnight.
         */
        if (start.isBefore(end)) {

            return !notificationTime.isBefore(start)
                    && notificationTime.isBefore(end);
        }

        /*
         * Cross-midnight window.
         *
         * Example:
         *
         * 22:00 -> 08:00
         */
        return !notificationTime.isBefore(start)
                || notificationTime.isBefore(end);
    }
}