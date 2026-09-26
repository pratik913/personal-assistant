package com.personalassistant.service;

import com.personalassistant.dto.NotificationResponse;
import com.personalassistant.dto.UnreadNotificationCountResponse;
import com.personalassistant.entity.Notification;
import com.personalassistant.entity.NotificationStatus;
import com.personalassistant.entity.NotificationType;
import com.personalassistant.entity.Task;
import com.personalassistant.entity.User;
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
     * Creates a task-start notification.
     *
     * The notification becomes visible 15 minutes
     * before the planned task starts.
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


        Task task =
                taskRepository
                        .findByIdAndUserId(
                                taskId,
                                userId
                        )
                        .orElseThrow(() ->
                                new IllegalStateException(
                                        "Task not found for notification."
                                )
                        );


        Instant scheduledAt =
                taskStartAt.minus(
                        TASK_START_REMINDER
                );


        /*
         * Do not create duplicate notifications
         * when the same plan is generated again.
         */
        boolean alreadyExists =
                notificationRepository
                        .existsByUserIdAndTaskIdAndTypeAndScheduledAt(
                                userId,
                                taskId,
                                NotificationType.TASK_STARTING,
                                scheduledAt
                        );


        if (alreadyExists) {
            return;
        }


        Notification notification =
                new Notification();


        notification.setUser(user);

        notification.setTask(task);

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
     * Returns notifications that are due now
     * and have not been read.
     */
    @Transactional(readOnly = true)
    public List<NotificationResponse> getUnreadNotifications(
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
                .map(notificationMapper::toResponse)
                .toList();

    }


    /**
     * Returns the number of notifications that
     * are currently visible to the user.
     */
    @Transactional(readOnly = true)
    public UnreadNotificationCountResponse getUnreadCount(
            UUID userId
    ) {

        long count =
                notificationRepository
                        .countByUserIdAndStatusAndScheduledAtLessThanEqual(
                                userId,
                                NotificationStatus.UNREAD,
                                Instant.now()
                        );


        return new UnreadNotificationCountResponse(
                count
        );

    }


    /**
     * Marks one notification as read.
     */
    public NotificationResponse markAsRead(
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


        if (
                notification.getStatus()
                        != NotificationStatus.READ
        ) {

            notification.setStatus(
                    NotificationStatus.READ
            );

            notification.setReadAt(
                    Instant.now()
            );

        }


        return notificationMapper.toResponse(
                notification
        );

    }

}