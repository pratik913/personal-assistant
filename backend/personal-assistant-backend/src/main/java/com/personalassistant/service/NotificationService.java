package com.personalassistant.service;

import com.personalassistant.dto.AiPlanItem;
import com.personalassistant.dto.NotificationDecision;
import com.personalassistant.dto.NotificationResponse;
import com.personalassistant.dto.UnreadNotificationCountResponse;
import com.personalassistant.entity.Notification;
import com.personalassistant.entity.NotificationPreference;
import com.personalassistant.entity.NotificationStatus;
import com.personalassistant.entity.NotificationType;
import com.personalassistant.entity.Task;
import com.personalassistant.entity.TaskPriority;
import com.personalassistant.entity.TaskStatus;
import com.personalassistant.entity.User;
import com.personalassistant.exception.TaskNotFoundException;
import com.personalassistant.exception.UserNotFoundException;
import com.personalassistant.mapper.NotificationMapper;
import com.personalassistant.repository.NotificationPreferenceRepository;
import com.personalassistant.repository.NotificationRepository;
import com.personalassistant.repository.TaskRepository;
import com.personalassistant.repository.UserRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

@Service
@Transactional
public class NotificationService {

    private static final Duration DEFAULT_TASK_START_REMINDER =
            Duration.ofMinutes(15);

    private final NotificationRepository notificationRepository;

    private final NotificationPreferenceRepository
            notificationPreferenceRepository;

    private final UserRepository userRepository;

    private final TaskRepository taskRepository;

    private final NotificationMapper notificationMapper;

    private final NotificationPolicyService
            notificationPolicyService;

    public NotificationService(
            NotificationRepository notificationRepository,
            NotificationPreferenceRepository notificationPreferenceRepository,
            UserRepository userRepository,
            TaskRepository taskRepository,
            NotificationMapper notificationMapper,
            NotificationPolicyService notificationPolicyService
    ) {

        this.notificationRepository =
                notificationRepository;

        this.notificationPreferenceRepository =
                notificationPreferenceRepository;

        this.userRepository =
                userRepository;

        this.taskRepository =
                taskRepository;

        this.notificationMapper =
                notificationMapper;

        this.notificationPolicyService =
                notificationPolicyService;
    }

    /**
     * Synchronizes task-start notifications with
     * the latest AI-generated plan.
     */
    public void createNotificationsForPlan(
            UUID userId,
            List<AiPlanItem> planItems
    ) {

        User user =
                userRepository
                        .findById(userId)
                        .orElseThrow(() ->
                                new UserNotFoundException(
                                        "User not found."
                                )
                        );

        if (
                planItems == null ||
                        planItems.isEmpty()
        ) {

            return;
        }

        for (
                AiPlanItem item :
                planItems
        ) {

            if (
                    item == null ||
                            item.taskId() == null ||
                            item.startAt() == null
            ) {

                continue;
            }

            synchronizeTaskNotification(
                    user,
                    item.taskId(),
                    item.startAt()
            );
        }
    }

    /**
     * Synchronizes the active unread notification
     * for a single task.
     */
    private void synchronizeTaskNotification(
            User user,
            UUID taskId,
            Instant taskStartAt
    ) {

        Task task =
                taskRepository
                        .findByIdAndUserId(
                                taskId,
                                user.getId()
                        )
                        .orElseThrow(() ->
                                new TaskNotFoundException(
                                        "Task not found."
                                )
                        );

        /*
         * Completed tasks should not have
         * active start reminders.
         */
        if (
                task.getStatus() ==
                        TaskStatus.COMPLETED
        ) {

            removeFutureUnreadNotifications(
                    taskId,
                    user.getId()
            );

            return;
        }

        NotificationPreference preference =
                notificationPreferenceRepository
                        .findByUserId(user.getId())
                        .orElse(null);

        /*
         * Use the user's configured reminder duration.
         */
        Duration reminderDuration =
                preference != null
                        ? Duration.ofMinutes(
                        preference.getReminderMinutes()
                )
                        : DEFAULT_TASK_START_REMINDER;

        Instant scheduledAt =
                taskStartAt.minus(
                        reminderDuration
                );

        /*
         * Ask the notification policy whether
         * this notification should exist.
         */
        NotificationDecision decision =
                notificationPolicyService
                        .evaluateTaskStartNotification(
                                task,
                                preference,
                                scheduledAt
                        );

        /*
         * Policy rejected the notification.
         *
         * Remove any future unread reminder for
         * this task so an old notification doesn't
         * remain active after preferences change.
         */
        if (
                !decision.shouldNotify()
        ) {

            removeFutureUnreadNotifications(
                    taskId,
                    user.getId()
            );

            return;
        }

        /*
         * Get ALL unread TASK_STARTING
         * notifications for this task.
         *
         * We deliberately do NOT filter by scheduledAt.
         */
        List<Notification> existingNotifications =
                notificationRepository
                        .findByTaskIdAndUserIdAndTypeAndStatus(
                                taskId,
                                user.getId(),
                                NotificationType.TASK_STARTING,
                                NotificationStatus.UNREAD
                        );

        boolean exactMatchExists = false;

        for (
                Notification notification :
                existingNotifications
        ) {

            if (
                    notification.getScheduledAt()
                            .equals(scheduledAt)
            ) {

                exactMatchExists = true;

                continue;
            }

            /*
             * Same task but old reminder time.
             *
             * Only one active unread reminder
             * is allowed.
             */
            notificationRepository.delete(
                    notification
            );
        }

        if (exactMatchExists) {

            return;
        }

        createTaskStartingNotification(
                user,
                task,
                scheduledAt,
                reminderDuration.toMinutes()
        );
    }

    /**
     * Public API used when a task receives
     * a scheduled start time directly.
     */
    public void createTaskStartingNotification(
            UUID userId,
            UUID taskId,
            Instant taskStartAt
    ) {

        User user =
                userRepository
                        .findById(userId)
                        .orElseThrow(() ->
                                new UserNotFoundException(
                                        "User not found."
                                )
                        );

        synchronizeTaskNotification(
                user,
                taskId,
                taskStartAt
        );
    }

    /**
     * Creates a task-start notification.
     */
    private void createTaskStartingNotification(
            User user,
            Task task,
            Instant scheduledAt,
            long reminderMinutes
    ) {

        boolean alreadyExists =
                notificationRepository
                        .existsByUserIdAndTaskIdAndTypeAndScheduledAt(
                                user.getId(),
                                task.getId(),
                                NotificationType.TASK_STARTING,
                                scheduledAt
                        );

        if (alreadyExists) {

            return;
        }

        Notification notification =
                new Notification();

        notification.setUser(
                user
        );

        notification.setTask(
                task
        );

        notification.setType(
                NotificationType.TASK_STARTING
        );

        notification.setStatus(
                NotificationStatus.UNREAD
        );

        notification.setTitle(
                buildNotificationTitle(task)
        );

        notification.setMessage(
                buildNotificationMessage(
                        task,
                        reminderMinutes
                )
        );

        notification.setScheduledAt(
                scheduledAt
        );

        notificationRepository.save(
                notification
        );
    }

    /**
     * Creates a notification title based
     * on task priority.
     */
    private String buildNotificationTitle(
            Task task
    ) {

        if (
                task.getPriority() ==
                        TaskPriority.HIGH
        ) {

            return "High priority task starting soon";
        }

        return "Task starting soon";
    }

    /**
     * Creates a priority-aware notification message.
     */
    private String buildNotificationMessage(
            Task task,
            long reminderMinutes
    ) {

        String duration =
                formatReminderDuration(
                        reminderMinutes
                );

        if (
                task.getPriority() ==
                        TaskPriority.HIGH
        ) {

            return "Your high priority task \""
                    + task.getTitle()
                    + "\" starts in "
                    + duration
                    + ".";
        }

        if (
                task.getPriority() ==
                        TaskPriority.MEDIUM
        ) {

            return "Your task \""
                    + task.getTitle()
                    + "\" starts in "
                    + duration
                    + ".";
        }

        return "Your task \""
                + task.getTitle()
                + "\" starts in "
                + duration
                + ".";
    }

    /**
     * Formats reminder duration for
     * human-readable notification text.
     */
    private String formatReminderDuration(
            long reminderMinutes
    ) {

        if (reminderMinutes < 60) {

            return reminderMinutes + " minutes";
        }

        long hours =
                reminderMinutes / 60;

        long remainingMinutes =
                reminderMinutes % 60;

        if (remainingMinutes == 0) {

            return hours == 1
                    ? "1 hour"
                    : hours + " hours";
        }

        return hours
                + " hours "
                + remainingMinutes
                + " minutes";
    }

    /**
     * Removes future unread notifications for
     * a task.
     */
    private void removeFutureUnreadNotifications(
            UUID taskId,
            UUID userId
    ) {

        notificationRepository
                .deleteByTaskIdAndUserIdAndStatusAndScheduledAtAfter(
                        taskId,
                        userId,
                        NotificationStatus.UNREAD,
                        Instant.now()
                );
    }

    /**
     * Returns currently due unread notifications.
     */
    @Transactional(readOnly = true)
    public List<NotificationResponse>
    getUnreadNotifications(
            UUID userId
    ) {

        Instant now =
                Instant.now();

        return notificationRepository
                .findByUserIdAndStatusAndScheduledAtLessThanEqualOrderByScheduledAtDesc(
                        userId,
                        NotificationStatus.UNREAD,
                        now
                )
                .stream()
                .filter(notification ->
                        notification.getTask()
                                .getStatus()
                                != TaskStatus.COMPLETED
                )
                .map(
                        notificationMapper::toResponse
                )
                .toList();
    }

    /**
     * Returns the number of currently
     * due unread notifications.
     */
    @Transactional(readOnly = true)
    public UnreadNotificationCountResponse
    getUnreadCount(
            UUID userId
    ) {

        Instant now =
                Instant.now();

        long count =
                notificationRepository
                        .findByUserIdAndStatusAndScheduledAtLessThanEqualOrderByScheduledAtDesc(
                                userId,
                                NotificationStatus.UNREAD,
                                now
                        )
                        .stream()
                        .filter(notification ->
                                notification.getTask()
                                        .getStatus()
                                        != TaskStatus.COMPLETED
                        )
                        .count();

        return new UnreadNotificationCountResponse(
                count
        );
    }

    /**
     * Marks a notification as read.
     */
    public NotificationResponse
    markAsRead(
            UUID userId,
            UUID notificationId
    ) {

        Notification notification =
                notificationRepository
                        .findByIdAndUserId(
                                notificationId,
                                userId
                        )
                        .orElseThrow(() ->
                                new IllegalStateException(
                                        "Notification not found."
                                )
                        );

        notification.setStatus(
                NotificationStatus.READ
        );

        notification.setReadAt(
                Instant.now()
        );

        Notification saved =
                notificationRepository.save(
                        notification
                );

        return notificationMapper.toResponse(
                saved
        );
    }

    /**
     * Removes future unread notifications
     * for a completed task.
     */
    public void
    removePendingNotificationsForCompletedTask(
            UUID userId,
            UUID taskId
    ) {

        removeFutureUnreadNotifications(
                taskId,
                userId
        );
    }

    /**
     * Removes all notifications belonging
     * to a task before that task is deleted.
     */
    public void
    deleteNotificationsForTask(
            UUID userId,
            UUID taskId
    ) {

        notificationRepository
                .deleteByTaskIdAndUserId(
                        taskId,
                        userId
                );
    }
}