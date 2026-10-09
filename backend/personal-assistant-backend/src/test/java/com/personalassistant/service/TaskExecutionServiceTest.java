package com.personalassistant.service;

import com.personalassistant.dto.CreateTaskExecutionRequest;
import com.personalassistant.dto.TaskExecutionResponse;
import com.personalassistant.dto.UpdateTaskExecutionRequest;
import com.personalassistant.entity.Notification;
import com.personalassistant.entity.Task;
import com.personalassistant.entity.TaskExecution;
import com.personalassistant.entity.TaskExecutionStatus;
import com.personalassistant.entity.User;
import com.personalassistant.exception.ConflictException;
import com.personalassistant.exception.TaskNotFoundException;
import com.personalassistant.exception.UserNotFoundException;
import com.personalassistant.mapper.TaskExecutionMapper;
import com.personalassistant.repository.TaskExecutionRepository;
import com.personalassistant.repository.TaskRepository;
import com.personalassistant.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class TaskExecutionServiceTest {

    private static final UUID USER_ID =
            UUID.fromString("11111111-1111-1111-1111-111111111111");

    private static final UUID TASK_ID =
            UUID.fromString("22222222-2222-2222-2222-222222222222");

    private static final UUID EXECUTION_ID =
            UUID.fromString("33333333-3333-3333-3333-333333333333");

    private static final Instant STARTED_AT =
            Instant.parse("2026-01-01T10:00:00Z");

    @Mock
    private TaskExecutionRepository taskExecutionRepository;

    @Mock
    private TaskRepository taskRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private TaskExecutionMapper taskExecutionMapper;

    @Mock
    private NotificationCorrelationService notificationCorrelationService;

    @InjectMocks
    private TaskExecutionService service;

    private User user;
    private Task task;

    @BeforeEach
    void setUp() {
        user = new User();
        user.setId(USER_ID);

        task = new Task();
        task.setId(TASK_ID);
        task.setUser(user);
    }

    @Test
    void startExecution_whenValidRequest_savesStartedExecution() {
        givenValidTaskAndUser();
        when(taskExecutionRepository.existsByTaskIdAndUserIdAndStatus(
                TASK_ID, USER_ID, TaskExecutionStatus.STARTED
        )).thenReturn(false);

        when(notificationCorrelationService.findNotificationForExecution(
                org.mockito.ArgumentMatchers.eq(USER_ID),
                org.mockito.ArgumentMatchers.eq(TASK_ID),
                any(Instant.class)
        )).thenReturn(Optional.empty());

        when(taskExecutionRepository.save(any(TaskExecution.class)))
                .thenAnswer(invocation -> {
                    TaskExecution execution = invocation.getArgument(0);
                    execution.setId(EXECUTION_ID);
                    return execution;
                });

        when(taskExecutionMapper.toResponse(any(TaskExecution.class)))
                .thenAnswer(invocation -> {
                    TaskExecution execution = invocation.getArgument(0);
                    return responseFrom(execution);
                });

        TaskExecutionResponse response = service.startExecution(
                TASK_ID,
                USER_ID,
                new CreateTaskExecutionRequest("Starting focused work")
        );

        assertNotNull(response);
        assertEquals(EXECUTION_ID, response.id());
        assertEquals(TASK_ID, response.taskId());
        assertEquals(TaskExecutionStatus.STARTED, response.status());
        assertEquals("Starting focused work", response.feedback());
        assertNotNull(response.startedAt());

        verify(taskExecutionRepository).save(any(TaskExecution.class));
    }

    @Test
    void startExecution_whenUserDoesNotExist_throwsUserNotFound() {
        when(userRepository.findById(USER_ID)).thenReturn(Optional.empty());

        assertThrows(
                UserNotFoundException.class,
                () -> service.startExecution(
                        TASK_ID, USER_ID, new CreateTaskExecutionRequest(null)
                )
        );

        verify(taskExecutionRepository, never()).save(any());
    }

    @Test
    void startExecution_whenTaskDoesNotBelongToUser_throwsTaskNotFound() {
        when(userRepository.findById(USER_ID)).thenReturn(Optional.of(user));
        when(taskRepository.findByIdAndUserId(TASK_ID, USER_ID))
                .thenReturn(Optional.empty());

        assertThrows(
                TaskNotFoundException.class,
                () -> service.startExecution(
                        TASK_ID, USER_ID, new CreateTaskExecutionRequest(null)
                )
        );

        verify(taskExecutionRepository, never()).save(any());
    }

    @Test
    void startExecution_whenTaskAlreadyRunning_throwsConflict() {
        givenValidTaskAndUser();
        when(taskExecutionRepository.existsByTaskIdAndUserIdAndStatus(
                TASK_ID, USER_ID, TaskExecutionStatus.STARTED
        )).thenReturn(true);

        assertThrows(
                ConflictException.class,
                () -> service.startExecution(
                        TASK_ID, USER_ID, new CreateTaskExecutionRequest(null)
                )
        );

        verify(notificationCorrelationService, never())
                .findNotificationForExecution(
                        any(UUID.class), any(UUID.class), any(Instant.class)
                );
        verify(taskExecutionRepository, never()).save(any());
    }

    @Test
    void startExecution_whenNotificationCorrelationThrows_stillSavesExecution() {
        givenValidTaskAndUser();
        when(taskExecutionRepository.existsByTaskIdAndUserIdAndStatus(
                TASK_ID, USER_ID, TaskExecutionStatus.STARTED
        )).thenReturn(false);

        when(notificationCorrelationService.findNotificationForExecution(
                org.mockito.ArgumentMatchers.eq(USER_ID),
                org.mockito.ArgumentMatchers.eq(TASK_ID),
                any(Instant.class)
        )).thenThrow(new IllegalStateException("Correlation unavailable"));

        when(taskExecutionRepository.save(any(TaskExecution.class)))
                .thenAnswer(invocation -> {
                    TaskExecution execution = invocation.getArgument(0);
                    execution.setId(EXECUTION_ID);
                    return execution;
                });

        when(taskExecutionMapper.toResponse(any(TaskExecution.class)))
                .thenAnswer(invocation -> responseFrom(invocation.getArgument(0)));

        TaskExecutionResponse response = service.startExecution(
                TASK_ID,
                USER_ID,
                new CreateTaskExecutionRequest(null)
        );

        assertNotNull(response);
        assertEquals(TaskExecutionStatus.STARTED, response.status());
        assertNull(response.notificationId());

        verify(taskExecutionRepository).save(any(TaskExecution.class));
    }

    @Test
    void startExecution_whenNotificationCorrelates_linksNotification() {
        givenValidTaskAndUser();
        when(taskExecutionRepository.existsByTaskIdAndUserIdAndStatus(
                TASK_ID, USER_ID, TaskExecutionStatus.STARTED
        )).thenReturn(false);

        Notification notification = new Notification();
        UUID notificationId = UUID.randomUUID();
        notification.setId(notificationId);

        when(notificationCorrelationService.findNotificationForExecution(
                org.mockito.ArgumentMatchers.eq(USER_ID),
                org.mockito.ArgumentMatchers.eq(TASK_ID),
                any(Instant.class)
        )).thenReturn(Optional.of(notification));

        when(taskExecutionRepository.save(any(TaskExecution.class)))
                .thenAnswer(invocation -> {
                    TaskExecution execution = invocation.getArgument(0);
                    execution.setId(EXECUTION_ID);
                    return execution;
                });

        when(taskExecutionMapper.toResponse(any(TaskExecution.class)))
                .thenAnswer(invocation -> responseFrom(invocation.getArgument(0)));

        TaskExecutionResponse response = service.startExecution(
                TASK_ID,
                USER_ID,
                new CreateTaskExecutionRequest(null)
        );

        assertEquals(notificationId, response.notificationId());
        verify(taskExecutionRepository).save(any(TaskExecution.class));
    }

    @Test
    void updateExecution_whenCompletingStartedExecution_setsEndTime() {
        TaskExecution execution = startedExecution();
        execution.setStartedAt(Instant.now().minusSeconds(60));

        when(taskExecutionRepository.findByIdAndTaskIdAndUserId(
                EXECUTION_ID, TASK_ID, USER_ID
        )).thenReturn(Optional.of(execution));

        when(taskExecutionRepository.save(any(TaskExecution.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        when(taskExecutionMapper.toResponse(any(TaskExecution.class)))
                .thenAnswer(invocation -> responseFrom(invocation.getArgument(0)));

        TaskExecutionResponse response = service.updateExecution(
                TASK_ID,
                EXECUTION_ID,
                USER_ID,
                new UpdateTaskExecutionRequest(
                        TaskExecutionStatus.COMPLETED,
                        "Finished"
                )
        );

        assertEquals(TaskExecutionStatus.COMPLETED, response.status());
        assertNotNull(response.endedAt());
        assertEquals("Finished", response.feedback());
    }
    @Test
    void updateExecution_whenRequestIsNull_throwsIllegalArgument() {
        when(taskExecutionRepository.findByIdAndTaskIdAndUserId(
                EXECUTION_ID, TASK_ID, USER_ID
        )).thenReturn(Optional.of(startedExecution()));

        assertThrows(
                IllegalArgumentException.class,
                () -> service.updateExecution(TASK_ID, EXECUTION_ID, USER_ID, null)
        );

        verify(taskExecutionRepository, never()).save(any());
    }

    @Test
    void updateExecution_whenTerminalStateIsChanged_rejectsTransition() {
        TaskExecution execution = startedExecution();
        execution.setStatus(TaskExecutionStatus.COMPLETED);
        execution.setEndedAt(STARTED_AT.plusSeconds(60));

        when(taskExecutionRepository.findByIdAndTaskIdAndUserId(
                EXECUTION_ID, TASK_ID, USER_ID
        )).thenReturn(Optional.of(execution));

        assertThrows(
                IllegalStateException.class,
                () -> service.updateExecution(
                        TASK_ID,
                        EXECUTION_ID,
                        USER_ID,
                        new UpdateTaskExecutionRequest(
                                TaskExecutionStatus.CANCELLED, null
                        )
                )
        );

        verify(taskExecutionRepository, never()).save(any());
    }

    @Test
    void calculateActualMinutes_returnsElapsedWholeMinutes() {
        TaskExecution execution = startedExecution();
        execution.setEndedAt(STARTED_AT.plusSeconds(185));

        assertEquals(3, service.calculateActualMinutes(execution));
    }

    @Test
    void calculateActualMinutes_whenExecutionIsNull_throwsIllegalArgument() {
        assertThrows(
                IllegalArgumentException.class,
                () -> service.calculateActualMinutes(null)
        );
    }

    @Test
    void calculateActualMinutes_whenNotCompleted_throwsIllegalState() {
        assertThrows(
                IllegalStateException.class,
                () -> service.calculateActualMinutes(startedExecution())
        );
    }

    @Test
    void calculateActualMinutes_whenEndPrecedesStart_throwsIllegalArgument() {
        TaskExecution execution = startedExecution();
        execution.setEndedAt(STARTED_AT.minusSeconds(60));

        assertThrows(
                IllegalArgumentException.class,
                () -> service.calculateActualMinutes(execution)
        );
    }

    @Test
    void calculateActualMinutes_whenDurationExceedsEightHours_throwsIllegalArgument() {
        TaskExecution execution = startedExecution();
        execution.setEndedAt(STARTED_AT.plusSeconds(8 * 60 * 60 + 60));

        assertThrows(
                IllegalArgumentException.class,
                () -> service.calculateActualMinutes(execution)
        );
    }

    @Test
    void getExecutions_whenTaskIsOwned_returnsMappedHistory() {
        TaskExecution execution = startedExecution();

        when(taskRepository.findByIdAndUserId(TASK_ID, USER_ID))
                .thenReturn(Optional.of(task));

        when(taskExecutionRepository.findByTaskIdAndUserIdOrderByStartedAtDesc(
                TASK_ID, USER_ID
        )).thenReturn(List.of(execution));

        when(taskExecutionMapper.toResponse(execution))
                .thenReturn(responseFrom(execution));

        List<TaskExecutionResponse> responses =
                service.getExecutions(TASK_ID, USER_ID);

        assertEquals(1, responses.size());
        assertEquals(TASK_ID, responses.get(0).taskId());
        verify(taskRepository).findByIdAndUserId(TASK_ID, USER_ID);
    }

    @Test
    void getExecutions_whenTaskIsNotOwned_throwsTaskNotFound() {
        when(taskRepository.findByIdAndUserId(TASK_ID, USER_ID))
                .thenReturn(Optional.empty());

        assertThrows(
                TaskNotFoundException.class,
                () -> service.getExecutions(TASK_ID, USER_ID)
        );

        verify(taskExecutionRepository, never())
                .findByTaskIdAndUserIdOrderByStartedAtDesc(TASK_ID, USER_ID);
    }

    private void givenValidTaskAndUser() {
        when(userRepository.findById(USER_ID)).thenReturn(Optional.of(user));
        when(taskRepository.findByIdAndUserId(TASK_ID, USER_ID))
                .thenReturn(Optional.of(task));
    }

    private TaskExecution startedExecution() {
        TaskExecution execution = new TaskExecution();
        execution.setId(EXECUTION_ID);
        execution.setTask(task);
        execution.setUser(user);
        execution.setStartedAt(STARTED_AT);
        execution.setStatus(TaskExecutionStatus.STARTED);
        execution.setCreatedAt(STARTED_AT);
        return execution;
    }

    private TaskExecutionResponse responseFrom(TaskExecution execution) {
        return new TaskExecutionResponse(
                execution.getId(),
                execution.getTask().getId(),
                execution.getStartedAt(),
                execution.getEndedAt(),
                execution.getStatus(),
                execution.getFeedback(),
                execution.getCreatedAt(),
                execution.getNotification() == null
                        ? null
                        : execution.getNotification().getId()
        );
    }
}