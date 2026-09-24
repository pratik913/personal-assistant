package com.personalassistant.entity;

import com.personalassistant.dto.ExecutionBlocker;
import com.personalassistant.dto.ExecutionDifficulty;
import com.personalassistant.dto.ExecutionTimeAssessment;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "execution_analyses")
@Getter
@Setter
public class ExecutionAnalysis {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @OneToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(
            name = "execution_id",
            nullable = false,
            unique = true
    )
    private TaskExecution execution;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private ExecutionDifficulty difficulty;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private ExecutionTimeAssessment timeAssessment;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private ExecutionBlocker blocker;

    @Column(length = 2000)
    private String insight;

    @Column(length = 2000)
    private String suggestion;

    @Column(nullable = false, updatable = false)
    private Instant createdAt;

    @PrePersist
    protected void onCreate() {
        createdAt = Instant.now();
    }
}