package com.personalassistant.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.personalassistant.ai.AiService;
import com.personalassistant.dto.AiPlanData;
import com.personalassistant.dto.AiPlanItem;
import com.personalassistant.dto.AiPlanResponse;
import com.personalassistant.dto.CreateAiPlanRequest;
import com.personalassistant.entity.AiPlan;
import com.personalassistant.entity.ScheduleEntry;
import com.personalassistant.entity.Task;
import com.personalassistant.entity.TaskStatus;
import com.personalassistant.entity.User;
import com.personalassistant.exception.UserNotFoundException;
import com.personalassistant.mapper.AiPlanMapper;
import com.personalassistant.repository.AiPlanRepository;
import com.personalassistant.repository.ScheduleEntryRepository;
import com.personalassistant.repository.TaskRepository;
import com.personalassistant.repository.UserRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.ZoneId;
import java.util.Comparator;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@Transactional
public class AiPlannerService {

    private final AiPlanRepository aiPlanRepository;
    private final UserRepository userRepository;
    private final TaskRepository taskRepository;
    private final ScheduleEntryRepository scheduleEntryRepository;
    private final AiService aiService;
    private final AiPlanMapper aiPlanMapper;
    private final ObjectMapper objectMapper;

    public AiPlannerService(
            AiPlanRepository aiPlanRepository,
            UserRepository userRepository,
            TaskRepository taskRepository,
            ScheduleEntryRepository scheduleEntryRepository,
            AiService aiService,
            AiPlanMapper aiPlanMapper,
            ObjectMapper objectMapper
    ) {
        this.aiPlanRepository = aiPlanRepository;
        this.userRepository = userRepository;
        this.taskRepository = taskRepository;
        this.scheduleEntryRepository = scheduleEntryRepository;
        this.aiService = aiService;
        this.aiPlanMapper = aiPlanMapper;
        this.objectMapper = objectMapper;
    }

    public AiPlanResponse createPlan(
            UUID userId,
            CreateAiPlanRequest request
    ) {

        User user = userRepository
                .findById(userId)
                .orElseThrow(() ->
                        new UserNotFoundException("User not found.")
                );

        /*
         * Get all tasks that are not completed.
         *
         * Completed tasks don't need to be considered
         * by the AI planner.
         */
        List<Task> tasks =
                taskRepository.findByUserIdAndStatusNot(
                        userId,
                        TaskStatus.COMPLETED
                );

        /*
         * Get the user's existing schedule.
         */
        List<ScheduleEntry> scheduleEntries =
                scheduleEntryRepository.findByUserId(userId);

        /*
         * Only schedule entries belonging to the
         * requested planning date are relevant.
         */
        List<ScheduleEntry> relevantScheduleEntries =
                filterScheduleForPlanningDate(
                        scheduleEntries,
                        request,
                        user
                );

        /*
         * Build the information that will be sent
         * to the AI planner.
         */
        String planningContext =
                buildPlanningContext(
                        request,
                        user,
                        tasks,
                        relevantScheduleEntries
                );

        /*
         * Ask the AI to generate a plan.
         */
        AiPlanData aiPlanData =
                aiService.generatePlan(planningContext);

        /*
         * Never trust AI output directly.
         *
         * Validate the generated plan against
         * our application rules.
         */
        validatePlan(
                aiPlanData,
                tasks,
                request,
                user,
                relevantScheduleEntries
        );

        /*
         * Convert the structured AI response into
         * JSON so it can be stored in PostgreSQL JSONB.
         */
        String planDataJson;

        try {

            planDataJson =
                    objectMapper.writeValueAsString(
                            aiPlanData
                    );

        } catch (JsonProcessingException exception) {

            throw new IllegalStateException(
                    "Unable to save AI plan data.",
                    exception
            );
        }

        /*
         * Create the database entity.
         */
        AiPlan aiPlan = new AiPlan();

        aiPlan.setUser(user);
        aiPlan.setSummary(
                buildSummary(aiPlanData)
        );
        aiPlan.setPlanData(planDataJson);

        /*
         * Persist the generated plan.
         */
        AiPlan savedPlan =
                aiPlanRepository.save(aiPlan);

        /*
         * Convert entity -> API response.
         */
        return aiPlanMapper.toResponse(savedPlan);
    }


    private List<ScheduleEntry> filterScheduleForPlanningDate(
            List<ScheduleEntry> scheduleEntries,
            CreateAiPlanRequest request,
            User user
    ) {

        ZoneId zoneId =
                ZoneId.of(user.getTimezone());

        return scheduleEntries.stream()
                .filter(entry ->
                        entry.getStartAt()
                                .atZone(zoneId)
                                .toLocalDate()
                                .equals(request.getPlanningDate())
                )
                .toList();
    }


    private String buildPlanningContext(
            CreateAiPlanRequest request,
            User user,
            List<Task> tasks,
            List<ScheduleEntry> scheduleEntries
    ) {

        StringBuilder context =
                new StringBuilder();

        context.append("User timezone: ")
                .append(user.getTimezone())
                .append("\n\n");

        context.append("Planning date: ")
                .append(request.getPlanningDate())
                .append("\n\n");

        context.append("Available time: ")
                .append(request.getAvailableFrom())
                .append(" - ")
                .append(request.getAvailableUntil())
                .append("\n\n");


        /*
         * Tasks available for planning.
         */
        context.append("Tasks:\n");

        tasks.forEach(task -> {

            context.append("- Task ID: ")
                    .append(task.getId())
                    .append("\n");

            context.append("  Title: ")
                    .append(task.getTitle())
                    .append("\n");

            context.append("  Priority: ")
                    .append(task.getPriority())
                    .append("\n");

            context.append("  Estimated minutes: ")
                    .append(task.getEstimatedMinutes())
                    .append("\n");

            context.append("  Status: ")
                    .append(task.getStatus())
                    .append("\n\n");
        });


        /*
         * Existing schedule that the AI must respect.
         */
        context.append(
                "Existing schedule for this date:\n"
        );

        if (scheduleEntries.isEmpty()) {

            context.append(
                    "- No existing scheduled tasks.\n"
            );

        } else {

            scheduleEntries.forEach(entry -> {

                context.append("- ")
                        .append(entry.getStartAt())
                        .append(" - ")
                        .append(entry.getEndAt())
                        .append(": ")
                        .append(entry.getTask().getTitle())
                        .append("\n");
            });
        }

        return context.toString();
    }


    private void validatePlan(
            AiPlanData plan,
            List<Task> availableTasks,
            CreateAiPlanRequest request,
            User user,
            List<ScheduleEntry> existingScheduleEntries
    ) {

        /*
         * Basic AI response validation.
         */
        if (plan == null || plan.items() == null) {

            throw new IllegalStateException(
                    "AI returned an invalid plan."
            );
        }


        /*
         * Get IDs of tasks that the user actually owns
         * and that are available for planning.
         */
        Set<UUID> availableTaskIds =
                availableTasks.stream()
                        .map(Task::getId)
                        .collect(Collectors.toSet());


        /*
         * Convert user's local planning window
         * into UTC Instants.
         */
        ZoneId zoneId =
                ZoneId.of(user.getTimezone());

        Instant availableFrom =
                request.getPlanningDate()
                        .atTime(request.getAvailableFrom())
                        .atZone(zoneId)
                        .toInstant();

        Instant availableUntil =
                request.getPlanningDate()
                        .atTime(request.getAvailableUntil())
                        .atZone(zoneId)
                        .toInstant();


        /*
         * Validate every AI-generated item.
         */
        for (AiPlanItem item : plan.items()) {

            /*
             * AI must only use tasks that actually exist
             * and belong to this user.
             */
            if (item.taskId() == null ||
                    !availableTaskIds.contains(item.taskId())) {

                throw new IllegalStateException(
                        "AI returned a task that is not available."
                );
            }


            /*
             * Start and end times must exist.
             */
            if (item.startAt() == null ||
                    item.endAt() == null) {

                throw new IllegalStateException(
                        "AI returned an invalid time range."
                );
            }


            /*
             * End must be after start.
             */
            if (!item.startAt()
                    .isBefore(item.endAt())) {

                throw new IllegalStateException(
                        "AI returned an invalid task duration."
                );
            }


            /*
             * AI must not schedule outside the
             * user's requested availability.
             */
            if (item.startAt().isBefore(availableFrom) ||
                    item.endAt().isAfter(availableUntil)) {

                throw new IllegalStateException(
                        "AI scheduled a task outside the available time."
                );
            }
        }


        /*
         * Make sure AI-generated tasks don't
         * overlap with each other.
         */
        validateNoOverlaps(plan.items());


        /*
         * Make sure AI-generated tasks don't
         * overlap with the user's existing schedule.
         */
        validateAgainstExistingSchedule(
                plan.items(),
                existingScheduleEntries
        );
    }


    private void validateNoOverlaps(
            List<AiPlanItem> items
    ) {

        /*
         * Sort tasks by start time.
         */
        List<AiPlanItem> sortedItems =
                items.stream()
                        .sorted(
                                Comparator.comparing(
                                        AiPlanItem::startAt
                                )
                        )
                        .toList();


        /*
         * Compare every task with the task
         * immediately before it.
         */
        for (int i = 1;
             i < sortedItems.size();
             i++) {

            AiPlanItem previous =
                    sortedItems.get(i - 1);

            AiPlanItem current =
                    sortedItems.get(i);


            if (current.startAt()
                    .isBefore(previous.endAt())) {

                throw new IllegalStateException(
                        "AI generated overlapping tasks."
                );
            }
        }
    }


    private void validateAgainstExistingSchedule(
            List<AiPlanItem> items,
            List<ScheduleEntry> existingScheduleEntries
    ) {

        /*
         * Compare every AI-generated task against
         * every existing schedule entry.
         */
        for (AiPlanItem item : items) {

            for (ScheduleEntry existingEntry :
                    existingScheduleEntries) {

                boolean overlaps =
                        item.startAt()
                                .isBefore(
                                        existingEntry.getEndAt()
                                )
                                &&
                                item.endAt()
                                        .isAfter(
                                                existingEntry.getStartAt()
                                        );


                if (overlaps) {

                    throw new IllegalStateException(
                            "AI scheduled a task that overlaps " +
                                    "with an existing schedule entry."
                    );
                }
            }
        }
    }


    private String buildSummary(
            AiPlanData aiPlanData
    ) {

        if (aiPlanData.items() == null ||
                aiPlanData.items().isEmpty()) {

            return "No tasks could be scheduled.";
        }

        return "AI generated a plan with "
                + aiPlanData.items().size()
                + " scheduled task(s).";
    }
}