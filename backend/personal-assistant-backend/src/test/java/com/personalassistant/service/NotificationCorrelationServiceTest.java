package com.personalassistant.service;

import com.personalassistant.entity.Notification;
import com.personalassistant.entity.NotificationStatus;
import com.personalassistant.entity.NotificationType;
import com.personalassistant.repository.NotificationRepository;
import com.personalassistant.repository.TaskExecutionRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import static org.mockito.Mockito.verifyNoInteractions;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class NotificationCorrelationServiceTest {

    private static final UUID USER_ID =
            UUID.fromString("11111111-1111-1111-1111-111111111111");

    private static final UUID TASK_ID =
            UUID.fromString("22222222-2222-2222-2222-222222222222");

    private static final Instant STARTED_AT =
            Instant.parse("2026-01-01T12:00:00Z");

    @Mock
    private NotificationRepository notificationRepository;

    @Mock
    private TaskExecutionRepository taskExecutionRepository;

    @InjectMocks
    private NotificationCorrelationService service;

    @Test
    void findNotificationForExecution_whenValidCandidateExists_returnsNotification() {
        Notification notification = notificationMinutesBeforeExecution(15);

        givenCandidates(notification);

        Optional<Notification> result =
                service.findNotificationForExecution(
                        USER_ID,
                        TASK_ID,
                        STARTED_AT
                );

        assertTrue(result.isPresent());
        assertEquals(notification.getId(), result.get().getId());

        verify(taskExecutionRepository)
                .existsByNotificationId(notification.getId());
    }



    @Test
    void findNotificationForExecution_whenNoCandidates_returnsEmpty() {
        givenCandidates();

        Optional<Notification> result =
                service.findNotificationForExecution(
                        USER_ID,
                        TASK_ID,
                        STARTED_AT
                );

        assertTrue(result.isEmpty());
    }

    @Test
    void findNotificationForExecution_whenNotificationWasNotRead_returnsEmpty() {
        Notification notification = notificationMinutesBeforeExecution(10);
        notification.setReadAt(null);

        givenCandidates(notification);

        Optional<Notification> result =
                service.findNotificationForExecution(
                        USER_ID,
                        TASK_ID,
                        STARTED_AT
                );

        assertTrue(result.isEmpty());
    }

    @Test
    void findNotificationForExecution_whenReadAfterExecutionStarted_skipsCandidate() {
        Notification notification = notificationMinutesBeforeExecution(10);
        notification.setReadAt(STARTED_AT.plusSeconds(1));

        givenCandidates(notification);

        Optional<Notification> result =
                service.findNotificationForExecution(
                        USER_ID,
                        TASK_ID,
                        STARTED_AT
                );

        assertTrue(result.isEmpty());
    }

    @Test
    void findNotificationForExecution_whenCandidateAlreadyCorrelated_skipsIt() {
        Notification alreadyCorrelated =
                notificationMinutesBeforeExecution(5);

        Notification available =
                notificationMinutesBeforeExecution(10);

        givenCandidates(alreadyCorrelated, available);

        when(taskExecutionRepository.existsByNotificationId(
                alreadyCorrelated.getId()
        )).thenReturn(true);

        Optional<Notification> result =
                service.findNotificationForExecution(
                        USER_ID,
                        TASK_ID,
                        STARTED_AT
                );

        assertTrue(result.isPresent());
        assertEquals(available.getId(), result.get().getId());

        verify(taskExecutionRepository)
                .existsByNotificationId(alreadyCorrelated.getId());

        verify(taskExecutionRepository)
                .existsByNotificationId(available.getId());
    }

    @Test
    void findNotificationForExecution_whenCandidateIsOlderThan24Hours_skipsIt() {
        Notification notification =
                notificationMinutesBeforeExecution(24 * 60 + 1);

        givenCandidates(notification);

        Optional<Notification> result =
                service.findNotificationForExecution(
                        USER_ID,
                        TASK_ID,
                        STARTED_AT
                );

        assertTrue(result.isEmpty());
    }

    @Test
    void findNotificationForExecution_whenCandidateIsExactly24HoursOld_acceptsIt() {
        Notification notification =
                notificationMinutesBeforeExecution(24 * 60);

        givenCandidates(notification);

        Optional<Notification> result =
                service.findNotificationForExecution(
                        USER_ID,
                        TASK_ID,
                        STARTED_AT
                );

        assertTrue(result.isPresent());
        assertEquals(notification.getId(), result.get().getId());
    }

    @Test
    void findNotificationForExecution_whenNewestCandidateIsInvalid_usesNextValidCandidate() {
        Notification newest =
                notificationMinutesBeforeExecution(5);

        // Its read timestamp is later than the execution start.
        newest.setReadAt(STARTED_AT.plusSeconds(60));

        Notification older =
                notificationMinutesBeforeExecution(10);

        givenCandidates(newest, older);

        Optional<Notification> result =
                service.findNotificationForExecution(
                        USER_ID,
                        TASK_ID,
                        STARTED_AT
                );

        assertTrue(result.isPresent());
        assertEquals(older.getId(), result.get().getId());
    }

    @Test
    void findNotificationForExecution_whenScheduledTimeIsNull_skipsCandidate() {
        Notification notification =
                notificationMinutesBeforeExecution(10);
        notification.setScheduledAt(null);

        givenCandidates(notification);

        Optional<Notification> result =
                service.findNotificationForExecution(
                        USER_ID,
                        TASK_ID,
                        STARTED_AT
                );

        assertTrue(result.isEmpty());
    }

    @Test
    void findNotificationForExecution_whenScheduledAfterExecution_skipsCandidate() {
        Notification notification =
                notificationMinutesBeforeExecution(10);

        notification.setScheduledAt(STARTED_AT.plusSeconds(60));
        notification.setReadAt(STARTED_AT.minusSeconds(60));

        givenCandidates(notification);

        Optional<Notification> result =
                service.findNotificationForExecution(
                        USER_ID,
                        TASK_ID,
                        STARTED_AT
                );

        assertTrue(result.isEmpty());
    }

    @Test
    void findNotificationForExecution_whenCandidateHasNullId_doesNotCheckExistingCorrelation() {
        Notification notification =
                notificationMinutesBeforeExecution(10);
        notification.setId(null);

        givenCandidates(notification);

        Optional<Notification> result =
                service.findNotificationForExecution(
                        USER_ID,
                        TASK_ID,
                        STARTED_AT
                );

        assertTrue(result.isPresent());
        assertFalse(result.get().getScheduledAt().isAfter(STARTED_AT));
    }

    private void givenCandidates(Notification... notifications) {
        when(notificationRepository
                .findByTaskIdAndUserIdAndTypeAndStatusAndScheduledAtLessThanEqualOrderByScheduledAtDesc(
                        TASK_ID,
                        USER_ID,
                        NotificationType.TASK_STARTING,
                        NotificationStatus.READ,
                        STARTED_AT
                )).thenReturn(List.of(notifications));
    }

    private Notification notificationMinutesBeforeExecution(int minutes) {
        Instant scheduledAt =
                STARTED_AT.minus(Duration.ofMinutes(minutes));

        Notification notification = new Notification();
        notification.setId(UUID.randomUUID());
        notification.setType(NotificationType.TASK_STARTING);
        notification.setStatus(NotificationStatus.READ);
        notification.setScheduledAt(scheduledAt);
        notification.setReadAt(scheduledAt.plusSeconds(30));

        return notification;
    }

    @Test
    void findNotificationForExecution_whenReadAtEqualsExecutionStart_acceptsCandidate() {
        Notification notification = notificationMinutesBeforeExecution(10);
        notification.setReadAt(STARTED_AT);

        givenCandidates(notification);

        Optional<Notification> result =
                service.findNotificationForExecution(
                        USER_ID,
                        TASK_ID,
                        STARTED_AT
                );

        assertTrue(result.isPresent());
        assertEquals(notification.getId(), result.get().getId());
    }

    @Test
    void findNotificationForExecution_whenStartedAtIsNull_returnsEmptyWithoutQuerying() {
        Optional<Notification> result =
                service.findNotificationForExecution(
                        USER_ID,
                        TASK_ID,
                        null
                );

        assertTrue(result.isEmpty());

        verifyNoInteractions(
                notificationRepository,
                taskExecutionRepository
        );
    }

}