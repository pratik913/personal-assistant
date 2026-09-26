package com.personalassistant.service;

import com.personalassistant.dto.AiPlanItem;
import com.personalassistant.dto.NotificationResponse;
import com.personalassistant.dto.UnreadNotificationCountResponse;
import com.personalassistant.entity.Notification;
import com.personalassistant.entity.NotificationStatus;
import com.personalassistant.entity.NotificationType;
import com.personalassistant.entity.Task;
import com.personalassistant.entity.TaskStatus;
import com.personalassistant.entity.User;
import com.personalassistant.exception.TaskNotFoundException;
import com.personalassistant.exception.UserNotFoundException;
import com.personalassistant.mapper.NotificationMapper;
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

    private static final Duration TASK_START_REMINDER =
            Duration.ofMinutes(15);

    private final NotificationRepository notificationRepository;

    private final UserRepository userRepository;

    private final TaskRepository taskRepository;

    private final NotificationMapper notificationMapper;

    public NotificationService(
            NotificationRepository notificationRepository,
            UserRepository userRepository,
            TaskRepository taskRepository,
            NotificationMapper notificationMapper
    ) {

        this.notificationRepository =
                notificationRepository;

        this.userRepository =
                userRepository;

        this.taskRepository =
                taskRepository;

        this.notificationMapper =
                notificationMapper;
    }


    /**
     * Synchronizes task-start notifications with
     * the latest AI-generated plan.
     *
     * Business rule:
     *
     * READ notifications are historical records
     * and are preserved.
     *
     * A task can have at most ONE UNREAD
     * TASK_STARTING notification.
     *
     * If the AI changes the task's scheduled time,
     * the old unread notification is removed and
     * a new notification is created.
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
     *
     * Rules:
     *
     * 1. Same task + same time:
     *    keep existing notification.
     *
     * 2. Same task + different time:
     *    delete old unread notification.
     *    create new notification.
     *
     * 3. READ notifications:
     *    never delete them.
     *
     * 4. COMPLETED task:
     *    remove pending unread notifications.
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

            notificationRepository
                    .deleteByTaskIdAndUserIdAndStatusAndScheduledAtAfter(
                            taskId,
                            user.getId(),
                            NotificationStatus.UNREAD,
                            Instant.now()
                    );

            return;
        }


        /*
         * Notification should appear 15 minutes
         * before the actual task start.
         */
        Instant scheduledAt =
                taskStartAt.minus(
                        TASK_START_REMINDER
                );


        /*
         * Get ALL unread TASK_STARTING
         * notifications for this task.
         *
         * We deliberately do NOT filter by
         * scheduledAt here.
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

            /*
             * The latest plan uses the same
             * reminder time.
             */
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
             * Delete it because only one
             * unread reminder is allowed.
             */
            notificationRepository.delete(
                    notification
            );
        }


        /*
         * Nothing more to do if the current
         * unread notification already matches
         * the latest plan.
         */
        if (exactMatchExists) {

            return;
        }


        /*
         * Create the new notification.
         */
        createTaskStartingNotification(
                user,
                task,
                scheduledAt
        );
    }


    /**
     * Creates or synchronizes a task-start
     * notification.
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
     * Internal notification creation method.
     */
    private void createTaskStartingNotification(
            User user,
            Task task,
            Instant scheduledAt
    ) {

        /*
         * Final exact duplicate protection.
         */
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
                "Task starting soon"
        );


        notification.setMessage(
                "Your task \""
                        + task.getTitle()
                        + "\" starts in 15 minutes."
        );


        notification.setScheduledAt(
                scheduledAt
        );


        notificationRepository.save(
                notification
        );
    }


    /**
     * Returns currently due unread
     * notifications.
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
     *
     * Ownership is verified using userId.
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

        notificationRepository
                .deleteByTaskIdAndUserIdAndStatusAndScheduledAtAfter(
                        taskId,
                        userId,
                        NotificationStatus.UNREAD,
                        Instant.now()
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