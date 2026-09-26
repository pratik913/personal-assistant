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

    private final TaskPlanningService taskPlanningService;

    private final NotificationService notificationService;


    public AiPlannerService(
            AiPlanRepository aiPlanRepository,
            UserRepository userRepository,
            TaskRepository taskRepository,
            ScheduleEntryRepository scheduleEntryRepository,
            AiService aiService,
            AiPlanMapper aiPlanMapper,
            ObjectMapper objectMapper,
            TaskPlanningService taskPlanningService,
            NotificationService notificationService
    ) {

        this.aiPlanRepository =
                aiPlanRepository;

        this.userRepository =
                userRepository;

        this.taskRepository =
                taskRepository;

        this.scheduleEntryRepository =
                scheduleEntryRepository;

        this.aiService =
                aiService;

        this.aiPlanMapper =
                aiPlanMapper;

        this.objectMapper =
                objectMapper;

        this.taskPlanningService =
                taskPlanningService;

        this.notificationService =
                notificationService;
    }


    public AiPlanResponse createPlan(
            UUID userId,
            CreateAiPlanRequest request
    ) {

        User user =
                userRepository
                        .findById(userId)
                        .orElseThrow(() ->
                                new UserNotFoundException(
                                        "User not found."
                                )
                        );


        /*
         * Get all tasks that are not completed.
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
                scheduleEntryRepository.findByUserId(
                        userId
                );


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
         * Build the information sent to AI.
         */
        String planningContext =
                buildPlanningContext(
                        request,
                        user,
                        tasks,
                        relevantScheduleEntries
                );


        /*
         * Ask AI to generate the plan.
         */
        AiPlanData aiPlanData =
                aiService.generatePlan(
                        planningContext
                );


        /*
         * Never trust AI output directly.
         */
        validatePlan(
                aiPlanData,
                tasks,
                request,
                user,
                relevantScheduleEntries
        );


        /*
         * Convert AI response to JSON.
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
         * Create database entity.
         */
        AiPlan aiPlan =
                new AiPlan();

        aiPlan.setUser(user);

        aiPlan.setSummary(
                buildSummary(
                        aiPlanData
                )
        );

        aiPlan.setPlanData(
                planDataJson
        );


        /*
         * Persist generated plan.
         */
        AiPlan savedPlan =
                aiPlanRepository.save(
                        aiPlan
                );


        /*
         * Synchronize task-start notifications
         * with the newly generated plan.
         *
         * This handles:
         *
         * 1. Existing matching notifications
         * 2. Obsolete future notifications
         * 3. Newly required notifications
         */
        createNotificationsForPlan(
                userId,
                aiPlanData
        );


        /*
         * Convert entity -> API response.
         */
        return aiPlanMapper.toResponse(
                savedPlan
        );
    }


    /**
     * Synchronizes task-start notifications with
     * the generated AI plan.
     *
     * NotificationService owns the actual
     * notification persistence and duplicate
     * handling logic.
     */
    private void createNotificationsForPlan(
            UUID userId,
            AiPlanData plan
    ) {

        if (
                plan == null ||
                        plan.items() == null
        ) {

            return;
        }


        /*
         * Pass the complete generated plan to
         * NotificationService.
         *
         * NotificationService decides:
         *
         * - which notifications already exist
         * - which future notifications are obsolete
         * - which notifications need to be created
         */
        notificationService
                .createNotificationsForPlan(
                        userId,
                        plan.items()
                );
    }


    private List<ScheduleEntry> filterScheduleForPlanningDate(
            List<ScheduleEntry> scheduleEntries,
            CreateAiPlanRequest request,
            User user
    ) {

        ZoneId zoneId =
                ZoneId.of(
                        user.getTimezone()
                );


        return scheduleEntries
                .stream()
                .filter(entry ->
                        entry.getStartAt()
                                .atZone(zoneId)
                                .toLocalDate()
                                .equals(
                                        request.getPlanningDate()
                                )
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


        context.append(
                        "User timezone: "
                )
                .append(
                        user.getTimezone()
                )
                .append("\n\n");


        context.append(
                        "Planning date: "
                )
                .append(
                        request.getPlanningDate()
                )
                .append("\n\n");


        context.append(
                        "Available time: "
                )
                .append(
                        request.getAvailableFrom()
                )
                .append(" - ")
                .append(
                        request.getAvailableUntil()
                )
                .append("\n\n");


        /*
         * Tasks available for planning.
         */
        context.append(
                "Tasks:\n"
        );


        tasks.forEach(task -> {

            context.append(
                            "- Task ID: "
                    )
                    .append(
                            task.getId()
                    )
                    .append("\n");


            context.append(
                            "  Title: "
                    )
                    .append(
                            task.getTitle()
                    )
                    .append("\n");


            context.append(
                            "  Priority: "
                    )
                    .append(
                            task.getPriority()
                    )
                    .append("\n");


            context.append(
                            "  Estimated minutes: "
                    )
                    .append(
                            task.getEstimatedMinutes()
                    )
                    .append("\n");


            context.append(
                            "  Status: "
                    )
                    .append(
                            task.getStatus()
                    )
                    .append("\n\n");

        });


        /*
         * Existing schedule.
         */
        context.append(
                "Existing schedule for this date:\n"
        );


        if (
                scheduleEntries.isEmpty()
        ) {

            context.append(
                    "- No existing scheduled tasks.\n"
            );

        } else {

            scheduleEntries.forEach(entry -> {

                context.append("- ")
                        .append(
                                entry.getStartAt()
                        )
                        .append(" - ")
                        .append(
                                entry.getEndAt()
                        )
                        .append(": ")
                        .append(
                                entry.getTask()
                                        .getTitle()
                        )
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

        if (
                plan == null ||
                        plan.items() == null
        ) {

            throw new IllegalStateException(
                    "AI returned an invalid plan."
            );
        }


        Set<UUID> availableTaskIds =
                availableTasks
                        .stream()
                        .map(Task::getId)
                        .collect(
                                Collectors.toSet()
                        );


        ZoneId zoneId =
                ZoneId.of(
                        user.getTimezone()
                );


        Instant availableFrom =
                request.getPlanningDate()
                        .atTime(
                                request.getAvailableFrom()
                        )
                        .atZone(zoneId)
                        .toInstant();


        Instant availableUntil =
                request.getPlanningDate()
                        .atTime(
                                request.getAvailableUntil()
                        )
                        .atZone(zoneId)
                        .toInstant();


        for (
                AiPlanItem item :
                plan.items()
        ) {

            if (
                    item.taskId() == null ||
                            !availableTaskIds.contains(
                                    item.taskId()
                            )
            ) {

                throw new IllegalStateException(
                        "AI returned a task that is not available."
                );
            }


            if (
                    item.startAt() == null ||
                            item.endAt() == null
            ) {

                throw new IllegalStateException(
                        "AI returned an invalid time range."
                );
            }


            if (
                    !item.endAt()
                            .isAfter(
                                    item.startAt()
                            )
            ) {

                throw new IllegalStateException(
                        "AI returned an invalid task duration."
                );
            }


            if (
                    item.startAt()
                            .isBefore(
                                    availableFrom
                            ) ||
                            item.endAt()
                                    .isAfter(
                                            availableUntil
                                    )
            ) {

                throw new IllegalStateException(
                        "AI scheduled a task outside the available window."
                );
            }

        }


        /*
         * Sort generated items by start time.
         */
        List<AiPlanItem> sortedItems =
                plan.items()
                        .stream()
                        .sorted(
                                Comparator.comparing(
                                        AiPlanItem::startAt
                                )
                        )
                        .toList();


        /*
         * Check overlap between AI-generated items.
         */
        for (
                int index = 1;
                index < sortedItems.size();
                index++
        ) {

            AiPlanItem previous =
                    sortedItems.get(
                            index - 1
                    );

            AiPlanItem current =
                    sortedItems.get(
                            index
                    );


            if (
                    current.startAt()
                            .isBefore(
                                    previous.endAt()
                            )
            ) {

                throw new IllegalStateException(
                        "AI generated overlapping tasks."
                );

            }

        }


        /*
         * Check overlap with existing schedule.
         */
        for (
                AiPlanItem item :
                plan.items()
        ) {

            for (
                    ScheduleEntry entry :
                    existingScheduleEntries
            ) {

                boolean overlaps =
                        item.startAt()
                                .isBefore(
                                        entry.getEndAt()
                                )
                                &&
                                item.endAt()
                                        .isAfter(
                                                entry.getStartAt()
                                        );


                if (overlaps) {

                    throw new IllegalStateException(
                            "AI generated a task that overlaps an existing schedule."
                    );

                }

            }

        }

    }


    private String buildSummary(
            AiPlanData plan
    ) {

        int count =
                plan.items() == null
                        ? 0
                        : plan.items().size();


        return "AI generated a plan with "
                + count
                + " scheduled task(s).";

    }

}