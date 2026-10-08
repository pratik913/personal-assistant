package com.personalassistant.service;

import com.personalassistant.dto.CreateAiPlanRequest;
import com.personalassistant.entity.ScheduleEntry;
import com.personalassistant.entity.Task;
import com.personalassistant.entity.TaskExecution;
import com.personalassistant.entity.TaskExecutionStatus;
import com.personalassistant.repository.TaskExecutionRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.ZoneId;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class SmartPlanningContextServiceTest {

    @Mock
    private TaskExecutionRepository taskExecutionRepository;

    private SmartPlanningContextService service;

    @BeforeEach
    void setUp() {
        service = new SmartPlanningContextService(
                taskExecutionRepository
        );
    }

    @Test
    void shouldCalculatePlanningCapacityAndBuffer() {

        UUID userId = UUID.randomUUID();

        CreateAiPlanRequest request =
                request(
                        LocalDate.of(2026, 10, 8),
                        LocalTime.of(9, 0),
                        LocalTime.of(17, 0)
                );

        String context =
                service.buildPlanningIntelligence(
                        userId,
                        request,
                        List.of(),
                        List.of(),
                        ZoneId.of("Asia/Kolkata")
                );

        assertTrue(
                context.contains(
                        "Available minutes: 480"
                )
        );

        assertTrue(
                context.contains(
                        "Existing scheduled minutes: 0"
                )
        );

        assertTrue(
                context.contains(
                        "Free minutes before buffer: 480"
                )
        );

        assertTrue(
                context.contains(
                        "Planning buffer: 48"
                )
        );

        assertTrue(
                context.contains(
                        "Usable planning capacity: 432"
                )
        );
    }

    @Test
    void shouldCalculateExistingScheduleMinutes() {

        UUID userId = UUID.randomUUID();

        ScheduleEntry first =
                mock(ScheduleEntry.class);

        ScheduleEntry second =
                mock(ScheduleEntry.class);

        when(first.getStartAt())
                .thenReturn(
                        Instant.parse(
                                "2026-10-08T04:00:00Z"
                        )
                );

        when(first.getEndAt())
                .thenReturn(
                        Instant.parse(
                                "2026-10-08T05:30:00Z"
                        )
                );

        when(second.getStartAt())
                .thenReturn(
                        Instant.parse(
                                "2026-10-08T06:00:00Z"
                        )
                );

        when(second.getEndAt())
                .thenReturn(
                        Instant.parse(
                                "2026-10-08T07:00:00Z"
                        )
                );

        CreateAiPlanRequest request =
                request(
                        LocalDate.of(2026, 10, 8),
                        LocalTime.of(9, 0),
                        LocalTime.of(17, 0)
                );

        String context =
                service.buildPlanningIntelligence(
                        userId,
                        request,
                        List.of(),
                        List.of(first, second),
                        ZoneId.of("Asia/Kolkata")
                );

        assertTrue(
                context.contains(
                        "Existing scheduled minutes: 150"
                )
        );

        assertTrue(
                context.contains(
                        "Free minutes before buffer: 330"
                )
        );

        assertTrue(
                context.contains(
                        "Planning buffer: 33"
                )
        );

        assertTrue(
                context.contains(
                        "Usable planning capacity: 297"
                )
        );
    }

    @Test
    void shouldUseCurrentEstimateWhenThereIsNoExecutionHistory() {

        UUID userId = UUID.randomUUID();

        UUID taskId = UUID.randomUUID();

        Task task =
                mock(Task.class);

        when(task.getId())
                .thenReturn(taskId);

        when(task.getEstimatedMinutes())
                .thenReturn(60);

        when(
                taskExecutionRepository
                        .findByTaskIdAndUserIdAndStatusOrderByStartedAtDesc(
                                taskId,
                                userId,
                                TaskExecutionStatus.COMPLETED
                        )
        ).thenReturn(List.of());

        CreateAiPlanRequest request =
                request(
                        LocalDate.of(2026, 10, 8),
                        LocalTime.of(9, 0),
                        LocalTime.of(17, 0)
                );

        String context =
                service.buildPlanningIntelligence(
                        userId,
                        request,
                        List.of(task),
                        List.of(),
                        ZoneId.of("Asia/Kolkata")
                );

        assertTrue(
                context.contains(
                        "Historical average: 60"
                )
        );

        assertTrue(
                context.contains(
                        "Recommended planning duration: 60"
                )
        );

        assertTrue(
                context.contains(
                        "Execution history used: 0"
                )
        );

        assertTrue(
                context.contains(
                        "Planning confidence: NONE"
                )
        );
    }

    @Test
    void shouldUseLowConfidenceWeightingForOneExecution() {

        UUID userId = UUID.randomUUID();

        UUID taskId = UUID.randomUUID();

        Task task =
                mock(Task.class);

        when(task.getId())
                .thenReturn(taskId);

        when(task.getEstimatedMinutes())
                .thenReturn(100);

        TaskExecution execution =
                mock(TaskExecution.class);

        Instant start =
                Instant.parse(
                        "2026-10-08T04:00:00Z"
                );

        Instant end =
                start.plusSeconds(
                        80 * 60L
                );

        when(execution.getStartedAt())
                .thenReturn(start);

        when(execution.getEndedAt())
                .thenReturn(end);

        when(
                taskExecutionRepository
                        .findByTaskIdAndUserIdAndStatusOrderByStartedAtDesc(
                                taskId,
                                userId,
                                TaskExecutionStatus.COMPLETED
                        )
        ).thenReturn(
                List.of(execution)
        );

        CreateAiPlanRequest request =
                request(
                        LocalDate.of(2026, 10, 8),
                        LocalTime.of(9, 0),
                        LocalTime.of(17, 0)
                );

        String context =
                service.buildPlanningIntelligence(
                        userId,
                        request,
                        List.of(task),
                        List.of(),
                        ZoneId.of("Asia/Kolkata")
                );

        assertTrue(
                context.contains(
                        "Historical average: 80"
                )
        );

        assertTrue(
                context.contains(
                        "Recommended planning duration: 95"
                )
        );

        assertTrue(
                context.contains(
                        "Execution history used: 1"
                )
        );

        assertTrue(
                context.contains(
                        "Planning confidence: LOW"
                )
        );
    }

    @Test
    void shouldUseMediumConfidenceWeightingForFourExecutions() {

        UUID userId = UUID.randomUUID();

        UUID taskId = UUID.randomUUID();

        Task task =
                mock(Task.class);

        when(task.getId())
                .thenReturn(taskId);

        when(task.getEstimatedMinutes())
                .thenReturn(100);

        TaskExecution execution1 =
                executionWithDuration(60);

        TaskExecution execution2 =
                executionWithDuration(70);

        TaskExecution execution3 =
                executionWithDuration(80);

        TaskExecution execution4 =
                executionWithDuration(90);

        when(
                taskExecutionRepository
                        .findByTaskIdAndUserIdAndStatusOrderByStartedAtDesc(
                                taskId,
                                userId,
                                TaskExecutionStatus.COMPLETED
                        )
        ).thenReturn(
                List.of(
                        execution1,
                        execution2,
                        execution3,
                        execution4
                )
        );

        CreateAiPlanRequest request =
                request(
                        LocalDate.of(2026, 10, 8),
                        LocalTime.of(9, 0),
                        LocalTime.of(17, 0)
                );

        String context =
                service.buildPlanningIntelligence(
                        userId,
                        request,
                        List.of(task),
                        List.of(),
                        ZoneId.of("Asia/Kolkata")
                );

        assertTrue(
                context.contains(
                        "Historical average: 75"
                )
        );

        assertTrue(
                context.contains(
                        "Recommended planning duration: 85"
                )
        );

        assertTrue(
                context.contains(
                        "Execution history used: 4"
                )
        );

        assertTrue(
                context.contains(
                        "Planning confidence: MEDIUM"
                )
        );
    }

    @Test
    void shouldUseHistoricalAverageForHighConfidence() {

        UUID userId = UUID.randomUUID();

        UUID taskId = UUID.randomUUID();

        Task task =
                mock(Task.class);

        when(task.getId())
                .thenReturn(taskId);

        when(task.getEstimatedMinutes())
                .thenReturn(120);

        TaskExecution execution1 =
                executionWithDuration(60);

        TaskExecution execution2 =
                executionWithDuration(70);

        TaskExecution execution3 =
                executionWithDuration(80);

        TaskExecution execution4 =
                executionWithDuration(90);

        TaskExecution execution5 =
                executionWithDuration(100);

        when(
                taskExecutionRepository
                        .findByTaskIdAndUserIdAndStatusOrderByStartedAtDesc(
                                taskId,
                                userId,
                                TaskExecutionStatus.COMPLETED
                        )
        ).thenReturn(
                List.of(
                        execution1,
                        execution2,
                        execution3,
                        execution4,
                        execution5
                )
        );

        CreateAiPlanRequest request =
                request(
                        LocalDate.of(2026, 10, 8),
                        LocalTime.of(9, 0),
                        LocalTime.of(17, 0)
                );

        String context =
                service.buildPlanningIntelligence(
                        userId,
                        request,
                        List.of(task),
                        List.of(),
                        ZoneId.of("Asia/Kolkata")
                );

        assertTrue(
                context.contains(
                        "Historical average: 80"
                )
        );

        assertTrue(
                context.contains(
                        "Recommended planning duration: 80"
                )
        );

        assertTrue(
                context.contains(
                        "Execution history used: 5"
                )
        );

        assertTrue(
                context.contains(
                        "Planning confidence: HIGH"
                )
        );
    }

    @Test
    void shouldIgnoreExecutionLongerThanEightHours() {

        UUID userId = UUID.randomUUID();

        UUID taskId = UUID.randomUUID();

        Task task =
                mock(Task.class);

        when(task.getId())
                .thenReturn(taskId);

        when(task.getEstimatedMinutes())
                .thenReturn(60);

        TaskExecution invalidExecution =
                executionWithDuration(481);

        when(
                taskExecutionRepository
                        .findByTaskIdAndUserIdAndStatusOrderByStartedAtDesc(
                                taskId,
                                userId,
                                TaskExecutionStatus.COMPLETED
                        )
        ).thenReturn(
                List.of(invalidExecution)
        );

        CreateAiPlanRequest request =
                request(
                        LocalDate.of(2026, 10, 8),
                        LocalTime.of(9, 0),
                        LocalTime.of(17, 0)
                );

        String context =
                service.buildPlanningIntelligence(
                        userId,
                        request,
                        List.of(task),
                        List.of(),
                        ZoneId.of("Asia/Kolkata")
                );

        assertTrue(
                context.contains(
                        "Historical average: 60"
                )
        );

        assertTrue(
                context.contains(
                        "Recommended planning duration: 60"
                )
        );

        assertTrue(
                context.contains(
                        "Execution history used: 0"
                )
        );

        assertTrue(
                context.contains(
                        "Planning confidence: NONE"
                )
        );
    }

    @Test
    void shouldIgnoreExecutionWithoutStartOrEnd() {

        UUID userId = UUID.randomUUID();

        UUID taskId = UUID.randomUUID();

        Task task =
                mock(Task.class);

        when(task.getId())
                .thenReturn(taskId);

        TaskExecution executionWithoutStart =
                mock(TaskExecution.class);

        /*
         * Deliberately stub ONLY startedAt.
         *
         * The production code checks startedAt first.
         * Because it is null, the invalid execution is rejected
         * before endedAt needs to be accessed.
         *
         * This avoids Mockito's unnecessary-stubbing failure.
         */
        when(executionWithoutStart.getStartedAt())
                .thenReturn(null);

        when(
                taskExecutionRepository
                        .findByTaskIdAndUserIdAndStatusOrderByStartedAtDesc(
                                taskId,
                                userId,
                                TaskExecutionStatus.COMPLETED
                        )
        ).thenReturn(
                List.of(executionWithoutStart)
        );

        CreateAiPlanRequest request =
                request(
                        LocalDate.of(2026, 10, 8),
                        LocalTime.of(9, 0),
                        LocalTime.of(17, 0)
                );

        String context =
                service.buildPlanningIntelligence(
                        userId,
                        request,
                        List.of(task),
                        List.of(),
                        ZoneId.of("Asia/Kolkata")
                );

        assertTrue(
                context.contains(
                        "Historical average: 0"
                )
        );

        assertTrue(
                context.contains(
                        "Recommended planning duration: 0"
                )
        );

        assertTrue(
                context.contains(
                        "Execution history used: 0"
                )
        );

        assertTrue(
                context.contains(
                        "Planning confidence: NONE"
                )
        );
    }

    @Test
    void shouldReturnZeroBufferWhenNoFreeCapacityExists() {

        UUID userId = UUID.randomUUID();

        CreateAiPlanRequest request =
                request(
                        LocalDate.of(2026, 10, 8),
                        LocalTime.of(9, 0),
                        LocalTime.of(17, 0)
                );

        ScheduleEntry schedule =
                mock(ScheduleEntry.class);

        when(schedule.getStartAt())
                .thenReturn(
                        Instant.parse(
                                "2026-10-08T03:30:00Z"
                        )
                );

        when(schedule.getEndAt())
                .thenReturn(
                        Instant.parse(
                                "2026-10-08T11:30:00Z"
                        )
                );

        String context =
                service.buildPlanningIntelligence(
                        userId,
                        request,
                        List.of(),
                        List.of(schedule),
                        ZoneId.of("Asia/Kolkata")
                );

        assertTrue(
                context.contains(
                        "Existing scheduled minutes: 480"
                )
        );

        assertTrue(
                context.contains(
                        "Free minutes before buffer: 0"
                )
        );

        assertTrue(
                context.contains(
                        "Planning buffer: 0"
                )
        );

        assertTrue(
                context.contains(
                        "Usable planning capacity: 0"
                )
        );
    }

    @Test
    void shouldRejectInvalidPlanningWindow() {

        UUID userId = UUID.randomUUID();

        CreateAiPlanRequest request =
                request(
                        LocalDate.of(2026, 10, 8),
                        LocalTime.of(17, 0),
                        LocalTime.of(9, 0)
                );

        assertThrows(
                IllegalArgumentException.class,
                () ->
                        service.buildPlanningIntelligence(
                                userId,
                                request,
                                List.of(),
                                List.of(),
                                ZoneId.of("Asia/Kolkata")
                        )
        );
    }

    private CreateAiPlanRequest request(
            LocalDate planningDate,
            LocalTime availableFrom,
            LocalTime availableUntil
    ) {

        CreateAiPlanRequest request =
                new CreateAiPlanRequest();

        request.setPlanningDate(
                planningDate
        );

        request.setAvailableFrom(
                availableFrom
        );

        request.setAvailableUntil(
                availableUntil
        );

        return request;
    }

    private TaskExecution executionWithDuration(
            long minutes
    ) {

        TaskExecution execution =
                mock(TaskExecution.class);

        Instant start =
                Instant.parse(
                        "2026-10-08T04:00:00Z"
                );

        Instant end =
                start.plusSeconds(
                        minutes * 60L
                );

        when(execution.getStartedAt())
                .thenReturn(start);

        when(execution.getEndedAt())
                .thenReturn(end);

        return execution;
    }
}