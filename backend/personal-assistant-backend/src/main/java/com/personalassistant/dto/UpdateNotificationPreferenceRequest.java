package com.personalassistant.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;

public record UpdateNotificationPreferenceRequest(

        Boolean taskStartNotificationsEnabled,

        @Min(
                value = 1,
                message = "Reminder minutes must be at least 1."
        )
        @Max(
                value = 1440,
                message = "Reminder minutes must not exceed 1440."
        )
        Integer reminderMinutes

) {
}