package com.personalassistant.service;

import com.personalassistant.dto.NotificationBehaviorInsightResponse;
import com.personalassistant.entity.Notification;
import com.personalassistant.entity.NotificationStatus;
import com.personalassistant.entity.NotificationType;
import com.personalassistant.entity.TaskExecution;
import com.personalassistant.entity.TaskExecutionStatus;
import com.personalassistant.repository.NotificationRepository;
import com.personalassistant.repository.TaskExecutionRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class NotificationBehaviorServiceTest {

    private static final UUID USER_ID =
            UUID.fromString("11111111-1111-1111-1111-111111111111");

    private static final Instant BASE_TIME =
            Instant.parse("2026-01-01T12:00:00Z");

    @Mock
    private NotificationRepository notificationRepository;

    @Mock
    private TaskExecutionRepository taskExecutionRepository;

    @InjectMocks
    private NotificationBehaviorService service;

    @BeforeEach
    void setUp() {
        when(notificationRepository
                .findByUserIdAndTypeOrderByScheduledAtDesc(
                        USER_ID,
                        NotificationType.TASK_STARTING
                )).thenReturn(List.of());

        when(taskExecutionRepository.findByUserIdAndStatus(
                USER_ID,
                TaskExecutionStatus.COMPLETED
        )).thenReturn(List.of());
    }

    @Test
    void getInsights_whenNoHistory_returnsZeroMetrics() {
        NotificationBehaviorInsightResponse response =
                service.getInsights(USER_ID);

        assertEquals(0L, response.totalReminders());
        assertEquals(0L, response.readReminders());
        assertEquals(0L, response.ignoredReminders());
        assertEquals(0L, response.actedOnReminders());
        assertEquals(0.0, response.readRate());
        assertEquals(0.0, response.actionRate());
        assertEquals(0.0, response.averageMinutesToStart());

        assertEquals(
                "MindMate needs more reminder history before it can learn your notification behavior.",
                response.insight()
        );
    }

    @Test
    void getInsights_whenRemindersHaveMixedStatuses_calculatesCountsAndRates() {
        Notification firstRead = notification(5, NotificationStatus.READ);
        Notification secondRead = notification(15, NotificationStatus.READ);
        Notification unread = notification(20, null);
        Notification anotherUnread = notification(25, null);

        givenNotifications(
                firstRead,
                secondRead,
                unread,
                anotherUnread
        );

        givenCompletedExecutions(
                execution(firstRead, 5),
                execution(secondRead, 15)
        );

        NotificationBehaviorInsightResponse response =
                service.getInsights(USER_ID);

        assertEquals(4L, response.totalReminders());
        assertEquals(2L, response.readReminders());
        assertEquals(2L, response.ignoredReminders());
        assertEquals(2L, response.actedOnReminders());

        assertEquals(50.0, response.readRate());
        assertEquals(50.0, response.actionRate());
        assertEquals(10.0, response.averageMinutesToStart());
    }

    @Test
    void getInsights_whenNoRemindersHaveBeenRead_returnsNoInteractionInsight() {
        Notification first = notification(10, null);
        Notification second = notification(20, null);

        givenNotifications(first, second);

        NotificationBehaviorInsightResponse response =
                service.getInsights(USER_ID);

        assertEquals(2L, response.totalReminders());
        assertEquals(0L, response.readReminders());
        assertEquals(2L, response.ignoredReminders());
        assertEquals(0L, response.actedOnReminders());
        assertEquals(0.0, response.readRate());
        assertEquals(0.0, response.actionRate());

        assertEquals(
                "Your reminders have not been opened yet. MindMate needs more interaction data to learn your preferred reminder timing.",
                response.insight()
        );
    }

    @Test
    void getInsights_whenRemindersAreReadButNotActedOn_returnsExecutionInsight() {
        Notification first = notification(10, NotificationStatus.READ);
        Notification second = notification(20, NotificationStatus.READ);

        givenNotifications(first, second);

        NotificationBehaviorInsightResponse response =
                service.getInsights(USER_ID);

        assertEquals(2L, response.totalReminders());
        assertEquals(2L, response.readReminders());
        assertEquals(0L, response.ignoredReminders());
        assertEquals(0L, response.actedOnReminders());
        assertEquals(100.0, response.readRate());
        assertEquals(0.0, response.actionRate());

        assertEquals(
                "Some reminders have been opened, but none are yet linked to a task execution. MindMate needs more execution data to learn your reminder timing.",
                response.insight()
        );
    }

    @Test
    void getInsights_whenExecutionHasNoNotification_ignoresExecution() {
        Notification reminder = notification(10, NotificationStatus.READ);
        givenNotifications(reminder);

        TaskExecution execution = execution(reminder, 10);
        execution.setNotification(null);

        givenCompletedExecutions(execution);

        NotificationBehaviorInsightResponse response =
                service.getInsights(USER_ID);

        assertEquals(1L, response.totalReminders());
        assertEquals(0L, response.actedOnReminders());
        assertEquals(0.0, response.actionRate());
        assertEquals(0.0, response.averageMinutesToStart());
    }

    @Test
    void getInsights_whenExecutionHasNullStartedAt_ignoresExecution() {
        Notification reminder = notification(10, NotificationStatus.READ);
        givenNotifications(reminder);

        TaskExecution execution = execution(reminder, 10);
        execution.setStartedAt(null);

        givenCompletedExecutions(execution);

        NotificationBehaviorInsightResponse response =
                service.getInsights(USER_ID);

        assertEquals(0L, response.actedOnReminders());
        assertEquals(0.0, response.actionRate());
        assertEquals(0.0, response.averageMinutesToStart());
    }

    @Test
    void getInsights_whenNotificationHasNullId_doesNotCountItAsActedOn() {
        Notification reminder = notification(10, NotificationStatus.READ);
        reminder.setId(null);

        givenNotifications(reminder);
        givenCompletedExecutions(execution(reminder, 10));

        NotificationBehaviorInsightResponse response =
                service.getInsights(USER_ID);

        assertEquals(1L, response.totalReminders());
        assertEquals(0L, response.actedOnReminders());
        assertEquals(0.0, response.actionRate());
        assertEquals(0.0, response.averageMinutesToStart());
    }

    @Test
    void getInsights_whenExecutionStartsBeforeScheduledTime_excludesResponseTime() {
        Notification reminder = notification(10, NotificationStatus.READ);
        givenNotifications(reminder);

        TaskExecution execution = new TaskExecution();
        execution.setNotification(reminder);
        execution.setStartedAt(
                reminder.getScheduledAt().minusSeconds(60)
        );

        givenCompletedExecutions(execution);

        NotificationBehaviorInsightResponse response =
                service.getInsights(USER_ID);

        assertEquals(1L, response.actedOnReminders());
        assertEquals(100.0, response.actionRate());
        assertEquals(0.0, response.averageMinutesToStart());
    }


    @Test
    void getInsights_whenScheduledAtIsNull_excludesResponseTime() {
        Notification reminder = notification(10, NotificationStatus.READ);
        reminder.setScheduledAt(null);

        givenNotifications(reminder);

        // Set the execution start directly because scheduledAt is null.
        TaskExecution execution = new TaskExecution();
        execution.setNotification(reminder);
        execution.setStartedAt(BASE_TIME);

        givenCompletedExecutions(execution);

        NotificationBehaviorInsightResponse response =
                service.getInsights(USER_ID);

        assertEquals(1L, response.totalReminders());
        assertEquals(1L, response.actedOnReminders());
        assertEquals(100.0, response.actionRate());
        assertEquals(0.0, response.averageMinutesToStart());
    }

    @Test
    void getInsights_whenDuplicateExecutionsExist_usesEarliestStartTime() {
        Notification reminder = notification(30, NotificationStatus.READ);
        givenNotifications(reminder);

        TaskExecution laterExecution = execution(reminder, 20);
        TaskExecution earlierExecution = execution(reminder, 10);

        givenCompletedExecutions(
                laterExecution,
                earlierExecution
        );

        NotificationBehaviorInsightResponse response =
                service.getInsights(USER_ID);

        assertEquals(1L, response.actedOnReminders());
        assertEquals(100.0, response.actionRate());
        assertEquals(10.0, response.averageMinutesToStart());
    }

    @Test
    void getInsights_whenResponseTimesHaveDifferentValues_calculatesRoundedAverage() {
        Notification first = notification(10, NotificationStatus.READ);
        Notification second = notification(20, NotificationStatus.READ);
        Notification third = notification(30, NotificationStatus.READ);

        givenNotifications(first, second, third);

        givenCompletedExecutions(
                execution(first, 1),
                execution(second, 2),
                execution(third, 2)
        );

        NotificationBehaviorInsightResponse response =
                service.getInsights(USER_ID);

        // (1 + 2 + 2) / 3 = 1.666..., rounded to 1.67.
        assertEquals(3L, response.actedOnReminders());
        assertEquals(100.0, response.actionRate());
        assertEquals(1.67, response.averageMinutesToStart());
    }

    @Test
    void getInsights_whenResponseTimeIsOverFiveMinutes_returnsSoonInsight() {
        Notification reminder = notification(10, NotificationStatus.READ);
        givenNotifications(reminder);
        givenCompletedExecutions(execution(reminder, 10));

        NotificationBehaviorInsightResponse response =
                service.getInsights(USER_ID);

        assertEquals(
                "You usually start tasks fairly soon after receiving reminders. MindMate can use this pattern to refine future reminder timing.",
                response.insight()
        );
    }

    @Test
    void getInsights_usesExpectedRepositoryQueries() {
        service.getInsights(USER_ID);

        verify(notificationRepository)
                .findByUserIdAndTypeOrderByScheduledAtDesc(
                        USER_ID,
                        NotificationType.TASK_STARTING
                );

        verify(taskExecutionRepository)
                .findByUserIdAndStatus(
                        USER_ID,
                        TaskExecutionStatus.COMPLETED
                );

        verifyNoMoreInteractions(
                notificationRepository,
                taskExecutionRepository
        );
    }

    private void givenNotifications(Notification... notifications) {
        when(notificationRepository
                .findByUserIdAndTypeOrderByScheduledAtDesc(
                        USER_ID,
                        NotificationType.TASK_STARTING
                )).thenReturn(List.of(notifications));
    }

    private void givenCompletedExecutions(TaskExecution... executions) {
        when(taskExecutionRepository.findByUserIdAndStatus(
                USER_ID,
                TaskExecutionStatus.COMPLETED
        )).thenReturn(List.of(executions));
    }

    private Notification notification(
            int minutesBeforeBaseTime,
            NotificationStatus status
    ) {
        Notification notification = new Notification();
        notification.setId(UUID.randomUUID());
        notification.setType(NotificationType.TASK_STARTING);
        notification.setStatus(status);
        notification.setScheduledAt(
                BASE_TIME.minus(Duration.ofMinutes(minutesBeforeBaseTime))
        );

        return notification;
    }

    private TaskExecution execution(
            Notification notification,
            int minutesAfterScheduledTime
    ) {
        TaskExecution execution = new TaskExecution();
        execution.setNotification(notification);
        execution.setStartedAt(
                notification.getScheduledAt()
                        .plus(Duration.ofMinutes(minutesAfterScheduledTime))
        );

        return execution;
    }
}