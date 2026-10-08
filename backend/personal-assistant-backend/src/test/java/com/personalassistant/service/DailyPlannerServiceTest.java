package com.personalassistant.service;

import com.personalassistant.ai.AiService;
import com.personalassistant.dto.AiPlanData;
import com.personalassistant.dto.AiPlanItem;
import com.personalassistant.dto.ApplyDailyPlanRequest;
import com.personalassistant.dto.CreateDailyPlanRequest;
import com.personalassistant.dto.ScheduleEntryResponse;
import com.personalassistant.entity.ScheduleEntry;
import com.personalassistant.entity.Task;
import com.personalassistant.entity.TaskStatus;
import com.personalassistant.entity.User;
import com.personalassistant.repository.ScheduleEntryRepository;
import com.personalassistant.repository.TaskRepository;
import com.personalassistant.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.web.server.ResponseStatusException;

import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.ZoneId;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class DailyPlannerServiceTest {

    @Mock
    private TaskRepository taskRepository;

    @Mock
    private ScheduleEntryRepository scheduleEntryRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private AiService aiService;

    @Mock
    private SmartPlanningContextService smartPlanningContextService;

    @Mock
    private ScheduleEntryService scheduleEntryService;

    @InjectMocks
    private DailyPlannerService dailyPlannerService;

    private UUID userId;
    private UUID taskId;
    private LocalDate planningDate;

    private User user;
    private Task task;

    @BeforeEach
    void setUp() {

        userId = UUID.randomUUID();

        taskId = UUID.randomUUID();

        planningDate =
                LocalDate.of(
                        2026,
                        10,
                        7
                );

        user =
                org.mockito.Mockito.mock(
                        User.class
                );

        task =
                org.mockito.Mockito.mock(
                        Task.class
                );
    }

    // ============================================================
    // GENERATE DAILY PLAN
    // ============================================================

    @Test
    void generateDailyPlan_shouldReturnValidProposal_withoutPersistingSchedule() {

        stubPlanningUserAndSchedule();

        when(task.getId())
                .thenReturn(taskId);

        when(task.getStatus())
                .thenReturn(TaskStatus.TODO);

        when(task.getEstimatedMinutes())
                .thenReturn(60);

        when(
                taskRepository.findByUserIdAndStatusNot(
                        eq(userId),
                        eq(TaskStatus.COMPLETED)
                )
        ).thenReturn(
                List.of(task)
        );

        stubSmartPlanningContext();

        CreateDailyPlanRequest request =
                validPlanningRequest();

        AiPlanData aiPlan =
                new AiPlanData(
                        planningDate,
                        List.of(
                                planItem(
                                        taskId,
                                        "09:00",
                                        "10:00"
                                )
                        )
                );

        when(aiService.generatePlan(any()))
                .thenReturn(aiPlan);

        AiPlanData result =
                dailyPlannerService.generateDailyPlan(
                        userId,
                        request
                );

        assertNotNull(result);

        assertEquals(
                planningDate,
                result.planningDate()
        );

        assertEquals(
                1,
                result.items().size()
        );

        verify(aiService)
                .generatePlan(any());

        verify(
                scheduleEntryService,
                never()
        ).createScheduleEntry(
                any(),
                any()
        );
    }

    @Test
    void generateDailyPlan_shouldRejectInvalidPlanningWindow() {

        CreateDailyPlanRequest request =
                new CreateDailyPlanRequest(
                        planningDate,
                        LocalTime.of(18, 0),
                        LocalTime.of(9, 0)
                );

        ResponseStatusException exception =
                assertThrows(
                        ResponseStatusException.class,
                        () ->
                                dailyPlannerService
                                        .generateDailyPlan(
                                                userId,
                                                request
                                        )
                );

        assertEquals(
                400,
                exception
                        .getStatusCode()
                        .value()
        );

        verify(
                aiService,
                never()
        ).generatePlan(any());
    }

    @Test
    void generateDailyPlan_shouldRejectUnknownTaskFromAi() {

        stubPlanningUserAndSchedule();

        when(task.getId())
                .thenReturn(taskId);

        when(
                taskRepository.findByUserIdAndStatusNot(
                        eq(userId),
                        eq(TaskStatus.COMPLETED)
                )
        ).thenReturn(
                List.of(task)
        );

        stubSmartPlanningContext();

        CreateDailyPlanRequest request =
                validPlanningRequest();

        UUID unknownTaskId =
                UUID.randomUUID();

        AiPlanData aiPlan =
                new AiPlanData(
                        planningDate,
                        List.of(
                                planItem(
                                        unknownTaskId,
                                        "09:00",
                                        "10:00"
                                )
                        )
                );

        when(aiService.generatePlan(any()))
                .thenReturn(aiPlan);

        ResponseStatusException exception =
                assertThrows(
                        ResponseStatusException.class,
                        () ->
                                dailyPlannerService
                                        .generateDailyPlan(
                                                userId,
                                                request
                                        )
                );

        assertEquals(
                400,
                exception
                        .getStatusCode()
                        .value()
        );

        verify(
                scheduleEntryService,
                never()
        ).createScheduleEntry(
                any(),
                any()
        );
    }

    @Test
    void generateDailyPlan_shouldRejectCompletedTask() {

        stubPlanningUserAndSchedule();

        when(task.getId())
                .thenReturn(taskId);

        when(task.getStatus())
                .thenReturn(TaskStatus.COMPLETED);

        when(
                taskRepository.findByUserIdAndStatusNot(
                        eq(userId),
                        eq(TaskStatus.COMPLETED)
                )
        ).thenReturn(
                List.of(task)
        );

        stubSmartPlanningContext();

        CreateDailyPlanRequest request =
                validPlanningRequest();

        AiPlanData aiPlan =
                new AiPlanData(
                        planningDate,
                        List.of(
                                planItem(
                                        taskId,
                                        "09:00",
                                        "10:00"
                                )
                        )
                );

        when(aiService.generatePlan(any()))
                .thenReturn(aiPlan);

        ResponseStatusException exception =
                assertThrows(
                        ResponseStatusException.class,
                        () ->
                                dailyPlannerService
                                        .generateDailyPlan(
                                                userId,
                                                request
                                        )
                );

        assertEquals(
                400,
                exception
                        .getStatusCode()
                        .value()
        );
    }

    @Test
    void generateDailyPlan_shouldRejectPlanOutsidePlanningWindow() {

        stubPlanningUserAndSchedule();

        when(task.getId())
                .thenReturn(taskId);

        when(task.getStatus())
                .thenReturn(TaskStatus.TODO);

        when(task.getEstimatedMinutes())
                .thenReturn(90);

        when(
                taskRepository.findByUserIdAndStatusNot(
                        eq(userId),
                        eq(TaskStatus.COMPLETED)
                )
        ).thenReturn(
                List.of(task)
        );

        stubSmartPlanningContext();

        CreateDailyPlanRequest request =
                validPlanningRequest();

        AiPlanData aiPlan =
                new AiPlanData(
                        planningDate,
                        List.of(
                                planItem(
                                        taskId,
                                        "08:00",
                                        "09:30"
                                )
                        )
                );

        when(aiService.generatePlan(any()))
                .thenReturn(aiPlan);

        ResponseStatusException exception =
                assertThrows(
                        ResponseStatusException.class,
                        () ->
                                dailyPlannerService
                                        .generateDailyPlan(
                                                userId,
                                                request
                                        )
                );

        assertEquals(
                400,
                exception
                        .getStatusCode()
                        .value()
        );
    }

    @Test
    void generateDailyPlan_shouldRejectOverlappingAiTasks() {

        stubPlanningUserAndSchedule();

        when(task.getId())
                .thenReturn(taskId);

        when(task.getStatus())
                .thenReturn(TaskStatus.TODO);

        when(task.getEstimatedMinutes())
                .thenReturn(60);

        UUID secondTaskId =
                UUID.randomUUID();

        Task secondTask =
                org.mockito.Mockito.mock(
                        Task.class
                );

        when(secondTask.getId())
                .thenReturn(secondTaskId);

        when(secondTask.getStatus())
                .thenReturn(TaskStatus.TODO);

        when(secondTask.getEstimatedMinutes())
                .thenReturn(60);

        when(
                taskRepository.findByUserIdAndStatusNot(
                        eq(userId),
                        eq(TaskStatus.COMPLETED)
                )
        ).thenReturn(
                List.of(
                        task,
                        secondTask
                )
        );

        stubSmartPlanningContext();

        CreateDailyPlanRequest request =
                validPlanningRequest();

        AiPlanData aiPlan =
                new AiPlanData(
                        planningDate,
                        List.of(
                                planItem(
                                        taskId,
                                        "09:00",
                                        "10:00"
                                ),
                                planItem(
                                        secondTaskId,
                                        "09:30",
                                        "10:30"
                                )
                        )
                );

        when(aiService.generatePlan(any()))
                .thenReturn(aiPlan);

        ResponseStatusException exception =
                assertThrows(
                        ResponseStatusException.class,
                        () ->
                                dailyPlannerService
                                        .generateDailyPlan(
                                                userId,
                                                request
                                        )
                );

        assertEquals(
                400,
                exception
                        .getStatusCode()
                        .value()
        );
    }

    @Test
    void generateDailyPlan_shouldRejectExistingScheduleConflict() {

        stubPlanningUserAndSchedule();

        when(task.getId())
                .thenReturn(taskId);

        when(task.getStatus())
                .thenReturn(TaskStatus.TODO);

        when(task.getEstimatedMinutes())
                .thenReturn(60);

        when(
                taskRepository.findByUserIdAndStatusNot(
                        eq(userId),
                        eq(TaskStatus.COMPLETED)
                )
        ).thenReturn(
                List.of(task)
        );

        stubSmartPlanningContext();

        ScheduleEntry existingEntry =
                org.mockito.Mockito.mock(
                        ScheduleEntry.class
                );

        Instant existingStart =
                Instant.parse(
                        "2026-10-07T04:00:00Z"
                );

        Instant existingEnd =
                Instant.parse(
                        "2026-10-07T05:00:00Z"
                );

        when(existingEntry.getStartAt())
                .thenReturn(existingStart);

        when(existingEntry.getEndAt())
                .thenReturn(existingEnd);

        Task scheduledTask =
                org.mockito.Mockito.mock(
                        Task.class
                );

        when(existingEntry.getTask())
                .thenReturn(scheduledTask);

        when(scheduledTask.getId())
                .thenReturn(
                        UUID.randomUUID()
                );

        when(scheduledTask.getTitle())
                .thenReturn(
                        "Existing task"
                );

        when(
                scheduleEntryRepository
                        .findByUserId(userId)
        ).thenReturn(
                List.of(existingEntry)
        );

        CreateDailyPlanRequest request =
                validPlanningRequest();

        AiPlanData aiPlan =
                new AiPlanData(
                        planningDate,
                        List.of(
                                planItem(
                                        taskId,
                                        "09:30",
                                        "10:30"
                                )
                        )
                );

        when(aiService.generatePlan(any()))
                .thenReturn(aiPlan);

        ResponseStatusException exception =
                assertThrows(
                        ResponseStatusException.class,
                        () ->
                                dailyPlannerService
                                        .generateDailyPlan(
                                                userId,
                                                request
                                        )
                );

        assertEquals(
                400,
                exception
                        .getStatusCode()
                        .value()
        );
    }

    // ============================================================
    // APPLY DAILY PLAN
    // ============================================================

    @Test
    void applyDailyPlan_shouldRejectTaskThatDoesNotBelongToUser() {

        when(userRepository.findById(userId))
                .thenReturn(
                        Optional.of(user)
                );

        when(user.getTimezone())
                .thenReturn(
                        "Asia/Kolkata"
                );

        when(
                scheduleEntryRepository
                        .findByUserId(userId)
        ).thenReturn(
                List.of()
        );

        UUID foreignTaskId =
                UUID.randomUUID();

        ApplyDailyPlanRequest request =
                new ApplyDailyPlanRequest(
                        planningDate,
                        LocalTime.of(9, 0),
                        LocalTime.of(18, 0),
                        List.of(
                                planItem(
                                        foreignTaskId,
                                        "09:00",
                                        "10:00"
                                )
                        )
                );

        when(
                taskRepository.findByUserId(userId)
        ).thenReturn(
                List.of()
        );

        ResponseStatusException exception =
                assertThrows(
                        ResponseStatusException.class,
                        () ->
                                dailyPlannerService
                                        .applyDailyPlan(
                                                userId,
                                                request
                                        )
                );

        assertEquals(
                400,
                exception
                        .getStatusCode()
                        .value()
        );

        verify(
                scheduleEntryService,
                never()
        ).createScheduleEntry(
                any(),
                any()
        );
    }

    @Test
    void applyDailyPlan_shouldRejectCompletedTask() {

        stubApplyUserAndSchedule();

        when(
                taskRepository.findByUserId(userId)
        ).thenReturn(
                List.of(task)
        );

        when(task.getId())
                .thenReturn(taskId);

        when(task.getStatus())
                .thenReturn(TaskStatus.COMPLETED);

        ApplyDailyPlanRequest request =
                new ApplyDailyPlanRequest(
                        planningDate,
                        LocalTime.of(9, 0),
                        LocalTime.of(18, 0),
                        List.of(
                                planItem(
                                        taskId,
                                        "09:00",
                                        "10:00"
                                )
                        )
                );

        ResponseStatusException exception =
                assertThrows(
                        ResponseStatusException.class,
                        () ->
                                dailyPlannerService
                                        .applyDailyPlan(
                                                userId,
                                                request
                                        )
                );

        assertEquals(
                400,
                exception
                        .getStatusCode()
                        .value()
        );

        verify(
                scheduleEntryService,
                never()
        ).createScheduleEntry(
                any(),
                any()
        );
    }
    @Test
    void applyDailyPlan_shouldRejectDuplicateTask() {

        stubApplyUserAndSchedule();

        AiPlanItem item =
                planItem(
                        taskId,
                        "09:00",
                        "10:00"
                );

        ApplyDailyPlanRequest request =
                new ApplyDailyPlanRequest(
                        planningDate,
                        LocalTime.of(9, 0),
                        LocalTime.of(18, 0),
                        List.of(
                                item,
                                item
                        )
                );

        ResponseStatusException exception =
                assertThrows(
                        ResponseStatusException.class,
                        () ->
                                dailyPlannerService
                                        .applyDailyPlan(
                                                userId,
                                                request
                                        )
                );

        assertEquals(
                400,
                exception
                        .getStatusCode()
                        .value()
        );

        verify(
                taskRepository,
                never()
        ).findByUserId(any());

        verify(
                scheduleEntryService,
                never()
        ).createScheduleEntry(
                any(),
                any()
        );
    }
    @Test
    void applyDailyPlan_shouldRejectItemOutsidePlanningWindow() {

        stubApplyUserAndSchedule();

        ApplyDailyPlanRequest request =
                new ApplyDailyPlanRequest(
                        planningDate,
                        LocalTime.of(9, 0),
                        LocalTime.of(18, 0),
                        List.of(
                                planItem(
                                        taskId,
                                        "08:00",
                                        "09:30"
                                )
                        )
                );

        ResponseStatusException exception =
                assertThrows(
                        ResponseStatusException.class,
                        () ->
                                dailyPlannerService
                                        .applyDailyPlan(
                                                userId,
                                                request
                                        )
                );

        assertEquals(
                400,
                exception
                        .getStatusCode()
                        .value()
        );

        verify(
                scheduleEntryService,
                never()
        ).createScheduleEntry(
                any(),
                any()
        );
    }

    @Test
    void applyDailyPlan_shouldPersistValidPlan() {

        stubApplyUserAndSchedule();

        when(
                taskRepository.findByUserId(userId)
        ).thenReturn(
                List.of(task)
        );

        when(task.getId())
                .thenReturn(taskId);

        when(task.getStatus())
                .thenReturn(TaskStatus.TODO);

        when(task.getEstimatedMinutes())
                .thenReturn(60);

        ApplyDailyPlanRequest request =
                new ApplyDailyPlanRequest(
                        planningDate,
                        LocalTime.of(9, 0),
                        LocalTime.of(18, 0),
                        List.of(
                                planItem(
                                        taskId,
                                        "09:00",
                                        "10:00"
                                )
                        )
                );

        ScheduleEntryResponse response =
                org.mockito.Mockito.mock(
                        ScheduleEntryResponse.class
                );

        when(
                scheduleEntryService
                        .createScheduleEntry(
                                eq(userId),
                                any()
                        )
        ).thenReturn(response);

        List<ScheduleEntryResponse> result =
                dailyPlannerService.applyDailyPlan(
                        userId,
                        request
                );

        assertNotNull(result);

        assertEquals(
                1,
                result.size()
        );

        assertEquals(
                response,
                result.get(0)
        );

        verify(
                scheduleEntryService
        ).createScheduleEntry(
                eq(userId),
                any()
        );
    }

    // ============================================================
    // TEST HELPERS
    // ============================================================

    private void stubPlanningUserAndSchedule() {

        when(userRepository.findById(userId))
                .thenReturn(
                        Optional.of(user)
                );

        when(user.getTimezone())
                .thenReturn(
                        "Asia/Kolkata"
                );

        when(
                scheduleEntryRepository
                        .findByUserId(userId)
        ).thenReturn(
                List.of()
        );
    }

    private void stubSmartPlanningContext() {

        when(
                smartPlanningContextService
                        .buildPlanningIntelligence(
                                eq(userId),
                                any(),
                                anyList(),
                                anyList(),
                                eq(
                                        ZoneId.of(
                                                "Asia/Kolkata"
                                        )
                                )
                        )
        ).thenReturn(
                "SMART PLANNING TEST CONTEXT"
        );
    }

    private void stubApplyUserAndSchedule() {

        when(userRepository.findById(userId))
                .thenReturn(
                        Optional.of(user)
                );

        when(user.getTimezone())
                .thenReturn(
                        "Asia/Kolkata"
                );

        when(
                scheduleEntryRepository
                        .findByUserId(userId)
        ).thenReturn(
                List.of()
        );
    }

    private CreateDailyPlanRequest validPlanningRequest() {

        return new CreateDailyPlanRequest(
                planningDate,
                LocalTime.of(9, 0),
                LocalTime.of(18, 0)
        );
    }

    private AiPlanItem planItem(
            UUID taskId,
            String start,
            String end
    ) {

        return new AiPlanItem(
                taskId,
                "Test task",
                localTimeToInstant(start),
                localTimeToInstant(end),
                "Test reason"
        );
    }

    private Instant localTimeToInstant(
            String time
    ) {

        LocalTime localTime =
                LocalTime.parse(time);

        return planningDate
                .atTime(localTime)
                .atZone(
                        ZoneId.of(
                                "Asia/Kolkata"
                        )
                )
                .toInstant();
    }
}