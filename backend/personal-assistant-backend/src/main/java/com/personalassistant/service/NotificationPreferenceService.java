package com.personalassistant.service;

import com.personalassistant.dto.NotificationPreferenceResponse;
import com.personalassistant.dto.UpdateNotificationPreferenceRequest;
import com.personalassistant.entity.NotificationPreference;
import com.personalassistant.entity.User;
import com.personalassistant.exception.UserNotFoundException;
import com.personalassistant.mapper.NotificationPreferenceMapper;
import com.personalassistant.repository.NotificationPreferenceRepository;
import com.personalassistant.repository.UserRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
@Transactional
public class NotificationPreferenceService {

    private static final boolean DEFAULT_TASK_START_NOTIFICATIONS_ENABLED = true;

    private static final int DEFAULT_REMINDER_MINUTES = 15;

    private final NotificationPreferenceRepository
            notificationPreferenceRepository;

    private final UserRepository userRepository;

    private final NotificationPreferenceMapper notificationPreferenceMapper;

    public NotificationPreferenceService(
            NotificationPreferenceRepository notificationPreferenceRepository,
            UserRepository userRepository,
            NotificationPreferenceMapper notificationPreferenceMapper
    ) {

        this.notificationPreferenceRepository =
                notificationPreferenceRepository;

        this.userRepository =
                userRepository;

        this.notificationPreferenceMapper =
                notificationPreferenceMapper;
    }

    /**
     * Returns the user's notification preferences.
     *
     * If the user does not have a preference row yet,
     * default preferences are created.
     */
    public NotificationPreferenceResponse getPreferences(
            UUID userId
    ) {

        NotificationPreference preference =
                getOrCreatePreference(userId);

        return notificationPreferenceMapper.toResponse(
                preference
        );
    }

    /**
     * Partially updates the user's notification preferences.
     *
     * Null fields are ignored because this is a PATCH operation.
     */
    public NotificationPreferenceResponse updatePreferences(
            UUID userId,
            UpdateNotificationPreferenceRequest request
    ) {

        NotificationPreference preference =
                getOrCreatePreference(userId);

        if (
                request.taskStartNotificationsEnabled()
                        != null
        ) {

            preference.setTaskStartNotificationsEnabled(
                    request.taskStartNotificationsEnabled()
            );
        }

        if (
                request.reminderMinutes()
                        != null
        ) {

            preference.setReminderMinutes(
                    request.reminderMinutes()
            );
        }

        NotificationPreference saved =
                notificationPreferenceRepository.save(
                        preference
                );

        return notificationPreferenceMapper.toResponse(
                saved
        );
    }

    /**
     * Returns an existing preference row or creates
     * one with application defaults.
     */
    private NotificationPreference getOrCreatePreference(
            UUID userId
    ) {

        return notificationPreferenceRepository
                .findByUserId(userId)
                .orElseGet(() -> createDefaultPreference(userId));
    }

    /**
     * Creates default notification preferences for a user.
     */
    private NotificationPreference createDefaultPreference(
            UUID userId
    ) {

        User user =
                userRepository
                        .findById(userId)
                        .orElseThrow(() ->
                                new UserNotFoundException(
                                        "User not found."
                                )
                        );

        NotificationPreference preference =
                new NotificationPreference();

        preference.setUser(user);

        preference.setTaskStartNotificationsEnabled(
                DEFAULT_TASK_START_NOTIFICATIONS_ENABLED
        );

        preference.setReminderMinutes(
                DEFAULT_REMINDER_MINUTES
        );

        return notificationPreferenceRepository.save(
                preference
        );
    }
}