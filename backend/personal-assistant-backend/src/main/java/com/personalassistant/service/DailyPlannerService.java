package com.personalassistant.service;

import com.personalassistant.ai.AiService;
import com.personalassistant.dto.AiPlanData;
import com.personalassistant.dto.AiPlanItem;
import com.personalassistant.dto.ApplyDailyPlanRequest;
import com.personalassistant.dto.CreateAiPlanRequest;
import com.personalassistant.dto.CreateDailyPlanRequest;
import com.personalassistant.dto.CreateScheduleEntryRequest;
import com.personalassistant.dto.ScheduleEntryResponse;
import com.personalassistant.entity.ScheduleEntry;
import com.personalassistant.entity.Task;
import com.personalassistant.entity.TaskStatus;
import com.personalassistant.entity.User;
import com.personalassistant.repository.ScheduleEntryRepository;
import com.personalassistant.repository.TaskRepository;
import com.personalassistant.repository.UserRepository;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.time.*;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

@Service
@Transactional
public class DailyPlannerService {

    private final TaskRepository taskRepository;

    private final ScheduleEntryRepository scheduleEntryRepository;

    private final UserRepository userRepository;

    private final AiService aiService;

    private final SmartPlanningContextService
            smartPlanningContextService;

    private final ScheduleEntryService
            scheduleEntryService;


    public DailyPlannerService(
            TaskRepository taskRepository,
            ScheduleEntryRepository scheduleEntryRepository,
            UserRepository userRepository,
            AiService aiService,
            SmartPlanningContextService smartPlanningContextService,
            ScheduleEntryService scheduleEntryService
    ) {

        this.taskRepository =
                taskRepository;

        this.scheduleEntryRepository =
                scheduleEntryRepository;

        this.userRepository =
                userRepository;

        this.aiService =
                aiService;

        this.smartPlanningContextService =
                smartPlanningContextService;

        this.scheduleEntryService =
                scheduleEntryService;
    }


    // =========================================================
    // GENERATE DAILY PLAN
    // =========================================================

    /**
     * Generates an AI daily-plan proposal.
     *
     * IMPORTANT:
     *
     * This method does NOT persist anything into
     * schedule_entries.
     *
     * The generated plan is returned to the frontend
     * as a proposal that requires explicit user confirmation.
     */
    @Transactional(readOnly = true)
    public AiPlanData generateDailyPlan(
            UUID userId,
            CreateDailyPlanRequest request
    ) {

        // -----------------------------------------------------
        // Validate planning window
        // -----------------------------------------------------

        validatePlanningWindow(request);


        // -----------------------------------------------------
        // Load authenticated user
        // -----------------------------------------------------

        User user =
                getUser(userId);


        // -----------------------------------------------------
        // Resolve user's timezone
        // -----------------------------------------------------

        ZoneId zoneId =
                getUserZone(user);


        LocalDate planningDate =
                request.planningDate();


        // -----------------------------------------------------
        // Convert local planning window to Instants
        // -----------------------------------------------------

        Instant availableFrom =
                toInstant(
                        planningDate,
                        request.availableFrom(),
                        zoneId
                );

        Instant availableUntil =
                toInstant(
                        planningDate,
                        request.availableUntil(),
                        zoneId
                );


        // -----------------------------------------------------
        // Load incomplete tasks
        // -----------------------------------------------------

        List<Task> tasks =
                taskRepository
                        .findByUserIdAndStatusNot(
                                userId,
                                TaskStatus.COMPLETED
                        );


        // -----------------------------------------------------
        // Load existing schedule
        // -----------------------------------------------------

        List<ScheduleEntry> allScheduleEntries =
                scheduleEntryRepository
                        .findByUserId(userId);


        // -----------------------------------------------------
        // IMPORTANT:
        //
        // Only schedule entries belonging to the requested
        // planning date should participate in daily planning.
        // -----------------------------------------------------

        List<ScheduleEntry> relevantScheduleEntries =
                filterScheduleForPlanningDate(
                        allScheduleEntries,
                        planningDate,
                        zoneId
                );


        // -----------------------------------------------------
        // Build normal planning context
        // -----------------------------------------------------

        String planningContext =
                buildPlanningContext(
                        user,
                        planningDate,
                        request,
                        tasks,
                        relevantScheduleEntries,
                        zoneId
                );


        // -----------------------------------------------------
        // Build SMART planning intelligence
        //
        // Reuse the existing SmartPlanningContextService.
        //
        // It already calculates:
        //
        // - historical duration
        // - recommended duration
        // - confidence
        // - planning capacity
        // - existing schedule usage
        // - planning buffer
        // -----------------------------------------------------

        CreateAiPlanRequest
                smartPlanningRequest =
                toCreateAiPlanRequest(request);


        String smartPlanningContext =
                smartPlanningContextService
                        .buildPlanningIntelligence(
                                userId,
                                smartPlanningRequest,
                                tasks,
                                relevantScheduleEntries,
                                zoneId
                        );


        // -----------------------------------------------------
        // Combine normal context + smart intelligence
        // -----------------------------------------------------

        String finalPlanningContext =
                planningContext
                        + "\n\n"
                        + smartPlanningContext;


        // -----------------------------------------------------
        // Ask AI to generate proposal
        //
        // IMPORTANT:
        //
        // AiService is used instead of OpenAiService directly.
        // This keeps the planner dependent on the abstraction.
        // -----------------------------------------------------

        AiPlanData plan =
                aiService.generatePlan(
                        finalPlanningContext
                );


        // -----------------------------------------------------
        // NEVER trust AI output directly
        //
        // Validate everything deterministically.
        // -----------------------------------------------------

        validateAiPlan(
                plan,
                planningDate,
                availableFrom,
                availableUntil,
                tasks,
                relevantScheduleEntries
        );


        return plan;
    }


    // =========================================================
    // APPLY DAILY PLAN
    // =========================================================

    /**
     * Applies a previously generated plan.
     *
     * The frontend proposal is NOT trusted.
     *
     * Ownership, task state, time range, duration and overlap
     * are validated again before anything is persisted.
     */
    public List<ScheduleEntryResponse> applyDailyPlan(
            UUID userId,
            ApplyDailyPlanRequest request
    ) {

        // -----------------------------------------------------
        // Validate request
        // -----------------------------------------------------

        if (request == null
                || request.items() == null
                || request.items().isEmpty()) {

            throw badRequest(
                    "Plan must contain at least one item."
            );
        }


        // -----------------------------------------------------
        // Revalidate the original planning window
        // -----------------------------------------------------

        validatePlanningWindow(
                request.planningDate(),
                request.availableFrom(),
                request.availableUntil()
        );


        User user =
                getUser(userId);


        ZoneId zoneId =
                getUserZone(user);


        Instant availableFrom =
                toInstant(
                        request.planningDate(),
                        request.availableFrom(),
                        zoneId
                );

        Instant availableUntil =
                toInstant(
                        request.planningDate(),
                        request.availableUntil(),
                        zoneId
                );


        // -----------------------------------------------------
        // Load existing schedule for the requested date
        // -----------------------------------------------------

        List<ScheduleEntry> existingSchedule =
                filterScheduleForPlanningDate(
                        scheduleEntryRepository
                                .findByUserId(userId),
                        request.planningDate(),
                        zoneId
                );


        // -----------------------------------------------------
        // Detect duplicate task IDs
        // -----------------------------------------------------

        Set<UUID> taskIds =
                new HashSet<>();


        for (AiPlanItem item :
                request.items()) {

            if (item == null) {

                throw badRequest(
                        "Plan contains a null item."
                );
            }


            if (item.taskId() == null) {

                throw badRequest(
                        "Every planned item must contain a taskId."
                );
            }


            if (!taskIds.add(
                    item.taskId()
            )) {

                throw badRequest(
                        "A task cannot appear more than once in a plan."
                );
            }
        }


        // -----------------------------------------------------
        // Load user's tasks
        //
        // Ownership is enforced by loading only the
        // authenticated user's tasks.
        // -----------------------------------------------------

        List<Task> tasks =
                taskRepository
                        .findByUserId(userId);


        Map<UUID, Task> taskMap =
                new HashMap<>();


        for (Task task : tasks) {

            taskMap.put(
                    task.getId(),
                    task
            );
        }


        // -----------------------------------------------------
        // Revalidate frontend proposal
        // -----------------------------------------------------

        validateAppliedItems(
                request.items(),
                taskMap,
                existingSchedule,
                availableFrom,
                availableUntil
        );


        // -----------------------------------------------------
        // Persist schedule entries
        //
        // ScheduleEntryService remains the single service
        // responsible for creating schedule entries.
        // -----------------------------------------------------

        return request.items()
                .stream()
                .map(item -> {

                    CreateScheduleEntryRequest
                            scheduleRequest =
                            new CreateScheduleEntryRequest();

                    scheduleRequest.setTaskId(
                            item.taskId()
                    );

                    scheduleRequest.setStartAt(
                            item.startAt()
                    );

                    scheduleRequest.setEndAt(
                            item.endAt()
                    );

                    return scheduleEntryService
                            .createScheduleEntry(
                                    userId,
                                    scheduleRequest
                            );
                })
                .toList();
    }


    // =========================================================
    // NORMAL PLANNING CONTEXT
    // =========================================================

    private String buildPlanningContext(
            User user,
            LocalDate planningDate,
            CreateDailyPlanRequest request,
            List<Task> tasks,
            List<ScheduleEntry> existingSchedule,
            ZoneId zoneId
    ) {

        StringBuilder context =
                new StringBuilder();


        // -----------------------------------------------------
        // User information
        // -----------------------------------------------------

        context.append(
                "USER PLANNING INFORMATION\n"
        );


        context.append(
                "Planning date: "
        ).append(
                planningDate
        ).append("\n");


        context.append(
                "User timezone: "
        ).append(
                zoneId
        ).append("\n");


        context.append(
                "Available from: "
        ).append(
                request.availableFrom()
        ).append("\n");


        context.append(
                "Available until: "
        ).append(
                request.availableUntil()
        ).append("\n\n");


        // -----------------------------------------------------
        // Available tasks
        // -----------------------------------------------------

        context.append(
                "AVAILABLE TASKS\n"
        );


        if (tasks.isEmpty()) {

            context.append(
                    "No incomplete tasks are available.\n"
            );

        } else {

            for (Task task : tasks) {

                context.append(
                        "Task ID: "
                ).append(
                        task.getId()
                ).append("\n");


                context.append(
                        "Title: "
                ).append(
                        safe(task.getTitle())
                ).append("\n");


                context.append(
                        "Description: "
                ).append(
                        safe(task.getDescription())
                ).append("\n");


                context.append(
                        "Priority: "
                ).append(
                        task.getPriority()
                ).append("\n");


                context.append(
                        "Due date: "
                ).append(
                        task.getDueDate() != null
                                ? task.getDueDate()
                                : "Not specified"
                ).append("\n");


                context.append(
                        "Estimated minutes: "
                ).append(
                        task.getEstimatedMinutes() != null
                                ? task.getEstimatedMinutes()
                                : "Not specified"
                ).append("\n\n");
            }
        }


        // -----------------------------------------------------
        // Existing schedule
        // -----------------------------------------------------

        context.append(
                "EXISTING SCHEDULE\n"
        );


        if (existingSchedule.isEmpty()) {

            context.append(
                    "No existing scheduled tasks on this date.\n"
            );

        } else {

            existingSchedule
                    .stream()
                    .sorted(
                            (first, second) ->
                                    first.getStartAt()
                                            .compareTo(
                                                    second.getStartAt()
                                            )
                    )
                    .forEach(entry -> {

                        context.append(
                                "Task ID: "
                        ).append(
                                entry.getTask().getId()
                        ).append("\n");


                        context.append(
                                "Task title: "
                        ).append(
                                safe(
                                        entry.getTask()
                                                .getTitle()
                                )
                        ).append("\n");


                        context.append(
                                "Start: "
                        ).append(
                                entry.getStartAt()
                                        .atZone(zoneId)
                        ).append("\n");


                        context.append(
                                "End: "
                        ).append(
                                entry.getEndAt()
                                        .atZone(zoneId)
                        ).append("\n\n");
                    });
        }


        // -----------------------------------------------------
        // Deterministic planning rules
        // -----------------------------------------------------

        context.append(
                "IMPORTANT PLANNING RULES\n"
        );


        context.append(
                "- Only schedule tasks listed in AVAILABLE TASKS.\n"
        );


        context.append(
                "- Do not schedule completed tasks.\n"
        );


        context.append(
                "- Do not create or invent tasks.\n"
        );


        context.append(
                "- Do not overlap existing schedule entries.\n"
        );


        context.append(
                "- Do not overlap proposed tasks.\n"
        );


        context.append(
                "- Keep every task inside the planning window.\n"
        );


        context.append(
                "- Respect task due dates.\n"
        );


        context.append(
                "- Prefer higher-priority tasks when time is limited.\n"
        );


        context.append(
                "- Use smart planning intelligence when deciding duration and capacity.\n"
        );


        context.append(
                "- Return only tasks that can actually fit.\n"
        );


        return context.toString();
    }


    // =========================================================
    // AI PLAN VALIDATION
    // =========================================================

    private void validateAiPlan(
            AiPlanData plan,
            LocalDate planningDate,
            Instant availableFrom,
            Instant availableUntil,
            List<Task> tasks,
            List<ScheduleEntry> existingSchedule
    ) {

        // -----------------------------------------------------
        // Basic response validation
        // -----------------------------------------------------

        if (plan == null) {

            throw badRequest(
                    "AI returned no planning proposal."
            );
        }


        if (plan.items() == null) {

            throw badRequest(
                    "AI returned a plan without items."
            );
        }


        // -----------------------------------------------------
        // Planning date validation
        // -----------------------------------------------------

        if (!planningDate.equals(
                plan.planningDate()
        )) {

            throw badRequest(
                    "AI returned an incorrect planning date."
            );
        }


        // -----------------------------------------------------
        // Build task ownership/availability map
        // -----------------------------------------------------

        Map<UUID, Task> taskMap =
                new HashMap<>();


        for (Task task : tasks) {

            taskMap.put(
                    task.getId(),
                    task
            );
        }


        // -----------------------------------------------------
        // Prevent duplicate tasks
        // -----------------------------------------------------

        Set<UUID> plannedTaskIds =
                new HashSet<>();


        // -----------------------------------------------------
        // Validate every AI item
        // -----------------------------------------------------

        for (AiPlanItem item :
                plan.items()) {

            if (item == null) {

                throw badRequest(
                        "AI returned a null plan item."
                );
            }


            UUID taskId =
                    item.taskId();


            if (taskId == null) {

                throw badRequest(
                        "AI returned a plan item without a task ID."
                );
            }


            if (!taskMap.containsKey(taskId)) {

                throw badRequest(
                        "AI attempted to schedule a task that is not available."
                );
            }


            Task task =
                    taskMap.get(taskId);


            if (task.getStatus() ==
                    TaskStatus.COMPLETED) {

                throw badRequest(
                        "AI attempted to schedule a completed task."
                );
            }


            if (!plannedTaskIds.add(
                    taskId
            )) {

                throw badRequest(
                        "AI scheduled the same task more than once."
                );
            }


            validatePlanTime(
                    item,
                    task,
                    availableFrom,
                    availableUntil
            );
        }


        // -----------------------------------------------------
        // Validate internal AI overlap
        // -----------------------------------------------------

        validateNoInternalOverlap(
                plan.items()
        );


        // -----------------------------------------------------
        // Validate overlap with existing schedule
        // -----------------------------------------------------

        validateNoExistingOverlap(
                plan.items(),
                existingSchedule
        );
    }


    // =========================================================
    // APPLY VALIDATION
    // =========================================================

    private void validateAppliedItems(
            List<AiPlanItem> items,
            Map<UUID, Task> taskMap,
            List<ScheduleEntry> existingSchedule,
            Instant availableFrom,
            Instant availableUntil
    ) {

        for (AiPlanItem item : items) {

            Task task =
                    taskMap.get(
                            item.taskId()
                    );


            // -------------------------------------------------
            // Ownership validation
            // -------------------------------------------------

            if (task == null) {

                throw badRequest(
                        "Task does not belong to the authenticated user."
                );
            }


            // -------------------------------------------------
            // Completed-task validation
            // -------------------------------------------------

            if (task.getStatus() ==
                    TaskStatus.COMPLETED) {

                throw badRequest(
                        "Completed tasks cannot be scheduled."
                );
            }


            // -------------------------------------------------
            // Time, planning-window and duration validation
            // -------------------------------------------------

            validatePlanTime(
                    item,
                    task,
                    availableFrom,
                    availableUntil
            );
        }


        // -----------------------------------------------------
        // Validate overlap between proposed tasks
        // -----------------------------------------------------

        validateNoInternalOverlap(
                items
        );


        // -----------------------------------------------------
        // Validate overlap with existing schedule
        // -----------------------------------------------------

        validateNoExistingOverlap(
                items,
                existingSchedule
        );
    }


    // =========================================================
    // TIME VALIDATION
    // =========================================================

    private void validatePlanTime(
            AiPlanItem item,
            Task task,
            Instant availableFrom,
            Instant availableUntil
    ) {

        if (item.startAt() == null
                || item.endAt() == null) {

            throw badRequest(
                    "Plan item requires valid startAt and endAt."
            );
        }


        if (!item.endAt().isAfter(
                item.startAt()
        )) {

            throw badRequest(
                    "Plan item endAt must be after startAt."
            );
        }


        // -----------------------------------------------------
        // Planning window
        // -----------------------------------------------------

        if (item.startAt().isBefore(
                availableFrom
        )
                || item.endAt().isAfter(
                availableUntil
        )) {

            throw badRequest(
                    "Plan item is outside the available planning window."
            );
        }


        // -----------------------------------------------------
        // Duration
        // -----------------------------------------------------

        long plannedMinutes =
                Duration.between(
                        item.startAt(),
                        item.endAt()
                ).toMinutes();


        if (plannedMinutes <= 0) {

            throw badRequest(
                    "Plan item must have a positive duration."
            );
        }


        // -----------------------------------------------------
        // Existing task estimate
        //
        // SmartPlanningContextService provides historical
        // intelligence to AI, but the current Task model only
        // stores estimatedMinutes as the authoritative maximum.
        // -----------------------------------------------------

        if (task.getEstimatedMinutes() != null
                && plannedMinutes >
                task.getEstimatedMinutes()) {

            throw badRequest(
                    "Plan item exceeds the task estimated duration."
            );
        }
    }


    // =========================================================
    // INTERNAL OVERLAP VALIDATION
    // =========================================================

    private void validateNoInternalOverlap(
            List<AiPlanItem> items
    ) {

        for (
                int firstIndex = 0;
                firstIndex < items.size();
                firstIndex++
        ) {

            AiPlanItem first =
                    items.get(firstIndex);


            for (
                    int secondIndex = firstIndex + 1;
                    secondIndex < items.size();
                    secondIndex++
            ) {

                AiPlanItem second =
                        items.get(secondIndex);


                if (overlaps(
                        first.startAt(),
                        first.endAt(),
                        second.startAt(),
                        second.endAt()
                )) {

                    throw badRequest(
                            "AI returned overlapping tasks."
                    );
                }
            }
        }
    }


    // =========================================================
    // EXISTING SCHEDULE OVERLAP VALIDATION
    // =========================================================

    private void validateNoExistingOverlap(
            List<AiPlanItem> items,
            List<ScheduleEntry> existingSchedule
    ) {

        for (AiPlanItem item : items) {

            for (ScheduleEntry existing :
                    existingSchedule) {

                if (overlaps(
                        item.startAt(),
                        item.endAt(),
                        existing.getStartAt(),
                        existing.getEndAt()
                )) {

                    throw badRequest(
                            "A proposed task overlaps an existing schedule entry."
                    );
                }
            }
        }
    }


    // =========================================================
    // OVERLAP HELPER
    // =========================================================

    private boolean overlaps(
            Instant firstStart,
            Instant firstEnd,
            Instant secondStart,
            Instant secondEnd
    ) {

        return firstStart.isBefore(secondEnd)
                && secondStart.isBefore(firstEnd);
    }


    // =========================================================
    // FILTER SCHEDULE BY PLANNING DATE
    // =========================================================
    private List<ScheduleEntry> filterScheduleForPlanningDate(
            List<ScheduleEntry> scheduleEntries,
            LocalDate planningDate,
            ZoneId zoneId
    ) {
        Instant dayStart = planningDate
                .atStartOfDay(zoneId)
                .toInstant();

        Instant nextDayStart = planningDate
                .plusDays(1)
                .atStartOfDay(zoneId)
                .toInstant();

        return scheduleEntries
                .stream()
                .filter(entry -> overlapsPlanningDate(
                        entry,
                        dayStart,
                        nextDayStart
                ))
                .toList();
    }
    private boolean overlapsPlanningDate(
            ScheduleEntry entry,
            Instant dayStart,
            Instant nextDayStart
    ) {
        if (entry == null) {
            return false;
        }

        Instant entryStart = entry.getStartAt();
        Instant entryEnd = entry.getEndAt();

        if (entryStart == null || entryEnd == null || !entryEnd.isAfter(entryStart)) {
            return false;
        }

        // Half-open interval overlap: [entryStart, entryEnd) intersects
        // [dayStart, nextDayStart). This includes overnight entries that
        // started before the planning date but continue into that date.
        return entryStart.isBefore(nextDayStart)
                && entryEnd.isAfter(dayStart);
    }




    // =========================================================
    // LOCAL DATE/TIME → INSTANT
    // =========================================================

    private Instant toInstant(
            LocalDate date,
            java.time.LocalTime time,
            ZoneId zoneId
    ) {

        return LocalDateTime
                .of(date, time)
                .atZone(zoneId)
                .toInstant();
    }


    // =========================================================
    // CREATE AI REQUEST FOR SMART PLANNING
    // =========================================================

    /**
     * The existing SmartPlanningContextService works with
     * CreateAiPlanRequest.
     *
     * Day 28 uses CreateDailyPlanRequest.
     *
     * Both represent the same planning-window information,
     * so we convert the daily request into the existing DTO
     * instead of changing the existing smart-planning API.
     */
    private CreateAiPlanRequest toCreateAiPlanRequest(
            CreateDailyPlanRequest request
    ) {

        CreateAiPlanRequest
                aiPlanRequest =
                new CreateAiPlanRequest();


        aiPlanRequest.setPlanningDate(
                request.planningDate()
        );


        aiPlanRequest.setAvailableFrom(
                request.availableFrom()
        );


        aiPlanRequest.setAvailableUntil(
                request.availableUntil()
        );


        return aiPlanRequest;
    }


    // =========================================================
    // PLANNING WINDOW VALIDATION
    // =========================================================

    private void validatePlanningWindow(
            CreateDailyPlanRequest request
    ) {

        if (request == null) {

            throw badRequest(
                    "Planning request cannot be null."
            );
        }


        validatePlanningWindow(
                request.planningDate(),
                request.availableFrom(),
                request.availableUntil()
        );
    }


    private void validatePlanningWindow(
            LocalDate planningDate,
            LocalTime availableFrom,
            LocalTime availableUntil
    ) {

        if (planningDate == null) {

            throw badRequest(
                    "Planning date is required."
            );
        }


        if (availableFrom == null) {

            throw badRequest(
                    "Available-from time is required."
            );
        }


        if (availableUntil == null) {

            throw badRequest(
                    "Available-until time is required."
            );
        }


        if (!availableUntil.isAfter(availableFrom)) {

            throw badRequest(
                    "availableUntil must be after availableFrom."
            );
        }
    }


    // =========================================================
    // USER
    // =========================================================

    private User getUser(
            UUID userId
    ) {

        return userRepository
                .findById(userId)
                .orElseThrow(() ->
                        badRequest(
                                "User not found."
                        )
                );
    }


    // =========================================================
    // USER TIMEZONE
    // =========================================================

    private ZoneId getUserZone(
            User user
    ) {

        try {

            return ZoneId.of(
                    user.getTimezone()
            );

        } catch (RuntimeException exception) {

            throw badRequest(
                    "User timezone is invalid."
            );
        }
    }


    // =========================================================
    // SAFE STRING
    // =========================================================

    private String safe(
            String value
    ) {

        return value == null
                || value.isBlank()
                ? "Not specified"
                : value;
    }


    // =========================================================
    // BAD REQUEST
    // =========================================================

    private ResponseStatusException badRequest(
            String message
    ) {

        return new ResponseStatusException(
                HttpStatus.BAD_REQUEST,
                message
        );
    }
}
