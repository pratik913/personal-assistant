package com.personalassistant.service;

import com.personalassistant.dto.NotificationTimingRecommendationResponse;
import com.personalassistant.dto.NotificationTimingRecommendationResponse.RecommendationConfidence;
import com.personalassistant.entity.Notification;
import com.personalassistant.entity.NotificationPreference;
import com.personalassistant.entity.NotificationType;
import com.personalassistant.entity.TaskExecution;
import com.personalassistant.entity.TaskExecutionStatus;
import com.personalassistant.repository.NotificationPreferenceRepository;
import com.personalassistant.repository.TaskExecutionRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class NotificationTimingRecommendationServiceTest {

    private static final UUID USER_ID =
            UUID.fromString("11111111-1111-1111-1111-111111111111");

    private static final Instant BASE_TIME =
            Instant.parse("2026-01-01T10:00:00Z");

    @Mock
    private NotificationPreferenceRepository notificationPreferenceRepository;

    @Mock
    private TaskExecutionRepository taskExecutionRepository;

    @InjectMocks
    private NotificationTimingRecommendationService service;

    @Test
    void getRecommendation_whenNoPreferenceAndNoHistory_usesDefaultReminder() {
        givenData(null, List.of());

        NotificationTimingRecommendationResponse response =
                service.getRecommendation(USER_ID);

        assertEquals(15, response.currentReminderMinutes());
        assertEquals(15, response.recommendedReminderMinutes());
        assertEquals(0, response.executionCount());
        assertEquals(0.0, response.averageMinutesToStart());
        assertEquals(RecommendationConfidence.NONE, response.confidence());
        assertNotNull(response.reason());
        assertTrue(response.reason().contains("more notification-to-execution history"));
    }

    @Test
    void getRecommendation_whenNoHistory_preservesConfiguredReminder() {
        givenData(preferenceWithReminder(45), List.of());

        NotificationTimingRecommendationResponse response =
                service.getRecommendation(USER_ID);

        assertEquals(45, response.currentReminderMinutes());
        assertEquals(45, response.recommendedReminderMinutes());
        assertEquals(0, response.executionCount());
        assertEquals(RecommendationConfidence.NONE, response.confidence());
    }

    @Test
    void getRecommendation_withOneExecution_usesConservativeRecommendation() {
        givenData(
                preferenceWithReminder(15),
                executionsWithResponseMinutes(4)
        );

        NotificationTimingRecommendationResponse response =
                service.getRecommendation(USER_ID);

        // Average 4 minutes maps to a base recommendation of 10.
        // LOW confidence moves halfway from 15 to 10: 12.5 -> 13.
        assertEquals(15, response.currentReminderMinutes());
        assertEquals(13, response.recommendedReminderMinutes());
        assertEquals(1, response.executionCount());
        assertEquals(4.0, response.averageMinutesToStart());
        assertEquals(RecommendationConfidence.LOW, response.confidence());
        assertTrue(response.reason().contains("conservative"));
    }

    @Test
    void getRecommendation_withThreeExecutions_usesMediumConfidence() {
        givenData(
                preferenceWithReminder(15),
                executionsWithResponseMinutes(20, 20, 20)
        );

        NotificationTimingRecommendationResponse response =
                service.getRecommendation(USER_ID);

        assertEquals(3, response.executionCount());
        assertEquals(20.0, response.averageMinutesToStart());
        assertEquals(RecommendationConfidence.MEDIUM, response.confidence());
        assertEquals(30, response.recommendedReminderMinutes());
    }

    @Test
    void getRecommendation_withSixExecutions_usesHighConfidence() {
        givenData(
                preferenceWithReminder(15),
                executionsWithResponseMinutes(70, 70, 70, 70, 70, 70)
        );

        NotificationTimingRecommendationResponse response =
                service.getRecommendation(USER_ID);

        assertEquals(6, response.executionCount());
        assertEquals(70.0, response.averageMinutesToStart());
        assertEquals(RecommendationConfidence.HIGH, response.confidence());
        assertEquals(60, response.recommendedReminderMinutes());
    }

    @ParameterizedTest
    @CsvSource({
            "0,   10",
            "5,   10",
            "6,   15",
            "15,  15",
            "16,  30",
            "30,  30",
            "31,  45",
            "60,  45",
            "61,  60",
            "120, 60",
            "121, 120",
            "500, 120"
    })
    void getRecommendation_usesExpectedReminderBuckets(
            int responseMinutes,
            int expectedRecommendation
    ) {
        givenData(
                preferenceWithReminder(15),
                executionsWithResponseMinutes(
                        responseMinutes,
                        responseMinutes,
                        responseMinutes
                )
        );

        NotificationTimingRecommendationResponse response =
                service.getRecommendation(USER_ID);

        assertEquals(RecommendationConfidence.MEDIUM, response.confidence());
        assertEquals(expectedRecommendation, response.recommendedReminderMinutes());
    }

    @Test
    void getRecommendation_roundsAverageToTwoDecimalPlaces() {
        givenData(
                preferenceWithReminder(15),
                executionsWithResponseMinutes(10, 11, 10)
        );

        NotificationTimingRecommendationResponse response =
                service.getRecommendation(USER_ID);

        assertEquals(3, response.executionCount());
        assertEquals(10.33, response.averageMinutesToStart());
        assertEquals(RecommendationConfidence.MEDIUM, response.confidence());
        assertEquals(15, response.recommendedReminderMinutes());
    }

    @Test
    void getRecommendation_countsOnlyExecutionsWithValidResponseTimes() {
        List<TaskExecution> executions = new ArrayList<>(
                executionsWithResponseMinutes(10, 20, 30)
        );

        // This execution is completed according to the repository result,
        // but it cannot contribute to the response-time calculation.
        executions.add(execution(startingNotification(), null));

        givenData(preferenceWithReminder(15), executions);

        NotificationTimingRecommendationResponse response =
                service.getRecommendation(USER_ID);

        assertEquals(3, response.executionCount());
        assertEquals(20.0, response.averageMinutesToStart());
        assertEquals(RecommendationConfidence.MEDIUM, response.confidence());
        assertEquals(30, response.recommendedReminderMinutes());
    }

    private void givenData(
            NotificationPreference preference,
            List<TaskExecution> executions
    ) {
        when(notificationPreferenceRepository.findByUserId(USER_ID))
                .thenReturn(Optional.ofNullable(preference));

        when(taskExecutionRepository.findByUserIdAndStatus(
                USER_ID,
                TaskExecutionStatus.COMPLETED
        )).thenReturn(executions);
    }

    private NotificationPreference preferenceWithReminder(int minutes) {
        NotificationPreference preference = new NotificationPreference();
        preference.setReminderMinutes(minutes);
        return preference;
    }

    private List<TaskExecution> executionsWithResponseMinutes(
            int... responseMinutes
    ) {
        List<TaskExecution> executions = new ArrayList<>();

        for (int minutes : responseMinutes) {
            Notification notification = startingNotification();

            TaskExecution execution = execution(
                    notification,
                    BASE_TIME.plusSeconds(minutes * 60L)
            );

            executions.add(execution);
        }

        return executions;
    }

    private Notification startingNotification() {
        Notification notification = new Notification();
        notification.setId(UUID.randomUUID());
        notification.setType(NotificationType.TASK_STARTING);
        notification.setScheduledAt(BASE_TIME);
        return notification;
    }

    private TaskExecution execution(
            Notification notification,
            Instant startedAt
    ) {
        TaskExecution execution = new TaskExecution();
        execution.setNotification(notification);
        execution.setStartedAt(startedAt);
        execution.setStatus(TaskExecutionStatus.COMPLETED);
        return execution;
    }

    private NotificationType otherNotificationType() {
        return java.util.Arrays.stream(NotificationType.values())
                .filter(type -> type != NotificationType.TASK_STARTING)
                .findFirst()
                .orElseThrow(() -> new IllegalStateException(
                        "A non-TASK_STARTING NotificationType is required for this test"
                ));
    }
}