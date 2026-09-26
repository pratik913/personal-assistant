package com.personalassistant.mapper;

import com.personalassistant.dto.NotificationPreferenceResponse;
import com.personalassistant.entity.NotificationPreference;
import org.springframework.stereotype.Component;

@Component
public class NotificationPreferenceMapper {

    public NotificationPreferenceResponse toResponse(
            NotificationPreference preference
    ) {

        return new NotificationPreferenceResponse(
                preference.isTaskStartNotificationsEnabled(),
                preference.getReminderMinutes(),
                preference.isQuietHoursEnabled(),
                preference.getQuietHoursStart(),
                preference.getQuietHoursEnd()
        );
    }
}