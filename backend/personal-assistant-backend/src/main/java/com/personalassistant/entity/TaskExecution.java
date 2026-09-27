package com.personalassistant.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.time.Instant;
import java.util.UUID;

@Getter
@Setter
@Entity
@Table(
        name = "task_executions",
        indexes = {
                @Index(
                        name = "idx_task_executions_notification_id",
                        columnList = "notification_id"
                )
        }
)
public class TaskExecution {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;


    /*
     * =====================================================
     * TASK
     * =====================================================
     */

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(
            name = "task_id",
            nullable = false
    )
    private Task task;


    /*
     * =====================================================
     * USER
     * =====================================================
     */

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(
            name = "user_id",
            nullable = false
    )
    private User user;


    /*
     * =====================================================
     * NOTIFICATION CORRELATION
     * =====================================================
     *
     * Optional because a user can start a task:
     *
     * - directly from the task page
     * - without opening a reminder
     * - before any reminder exists
     *
     * When an execution can be confidently linked to a
     * TASK_STARTING notification, this field stores that
     * relationship.
     */

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(
            name = "notification_id"
    )
    private Notification notification;


    /*
     * =====================================================
     * EXECUTION TIMING
     * =====================================================
     */

    @Column(
            nullable = false
    )
    private Instant startedAt;


    private Instant endedAt;


    /*
     * =====================================================
     * STATUS
     * =====================================================
     */

    @Enumerated(EnumType.STRING)
    @Column(
            nullable = false
    )
    private TaskExecutionStatus status;


    /*
     * =====================================================
     * FEEDBACK
     * =====================================================
     */

    @Column(
            length = 2000
    )
    private String feedback;


    /*
     * =====================================================
     * CREATED
     * =====================================================
     */

    @Column(
            nullable = false,
            updatable = false
    )
    private Instant createdAt;


    @PrePersist
    protected void onCreate() {

        if (createdAt == null) {

            createdAt = Instant.now();

        }

    }

}