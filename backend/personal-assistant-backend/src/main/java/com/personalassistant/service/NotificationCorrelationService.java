package com.personalassistant.service;

import com.personalassistant.entity.Notification;
import com.personalassistant.entity.NotificationStatus;
import com.personalassistant.entity.NotificationType;
import com.personalassistant.repository.NotificationRepository;
import com.personalassistant.repository.TaskExecutionRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Service
@Transactional(readOnly = true)
public class NotificationCorrelationService {

    /*
     * A reminder can be configured up to 1440 minutes
     * (24 hours), so 24 hours is the maximum valid
     * correlation window.
     */
    private static final Duration MAX_CORRELATION_WINDOW =
            Duration.ofHours(24);


    private final NotificationRepository notificationRepository;

    private final TaskExecutionRepository taskExecutionRepository;


    public NotificationCorrelationService(
            NotificationRepository notificationRepository,
            TaskExecutionRepository taskExecutionRepository
    ) {

        this.notificationRepository =
                notificationRepository;

        this.taskExecutionRepository =
                taskExecutionRepository;

    }


    /**
     * Finds the notification that most likely led to
     * the current task execution.
     *
     * Correlation rules:
     *
     * 1. Same user.
     * 2. Same task.
     * 3. TASK_STARTING notification.
     * 4. Notification must have been READ.
     * 5. Notification must have been scheduled before
     *    the execution started.
     * 6. Notification must have been read before execution.
     * 7. Notification must not already belong to another
     *    execution.
     * 8. Reminder -> execution duration must be <= 24 hours.
     */
    public Optional<Notification> findNotificationForExecution(
            UUID userId,
            UUID taskId,
            Instant startedAt
    ) {
        if (startedAt == null) {
            return Optional.empty();
        }
        List<Notification> candidates =
                notificationRepository
                        .findByTaskIdAndUserIdAndTypeAndStatusAndScheduledAtLessThanEqualOrderByScheduledAtDesc(
                                taskId,
                                userId,
                                NotificationType.TASK_STARTING,
                                NotificationStatus.READ,
                                startedAt
                        );


        for (
                Notification notification :
                candidates
        ) {

            if (
                    notification.getScheduledAt() == null
            ) {

                continue;

            }


            /*
             * A notification must actually have been
             * opened/read before the task execution started.
             */
            if (
                    notification.getReadAt() == null ||
                            notification.getReadAt()
                                    .isAfter(startedAt)
            ) {

                continue;

            }


            /*
             * Do not reuse the same notification for
             * another execution.
             */
            if (
                    notification.getId() != null &&
                            taskExecutionRepository
                                    .existsByNotificationId(
                                            notification.getId()
                                    )
            ) {

                continue;

            }


            Duration responseTime =
                    Duration.between(
                            notification.getScheduledAt(),
                            startedAt
                    );


            /*
             * Defensive validation.
             */
            if (
                    responseTime.isNegative()
            ) {

                continue;

            }


            if (
                    responseTime.compareTo(
                            MAX_CORRELATION_WINDOW
                    ) > 0
            ) {

                continue;

            }


            /*
             * Candidates are ordered newest first,
             * therefore the first valid candidate is
             * the closest reminder to the execution.
             */
            return Optional.of(notification);

        }


        return Optional.empty();

    }

}