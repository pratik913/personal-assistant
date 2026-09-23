package com.personalassistant.mapper;

import com.personalassistant.dto.TaskExecutionResponse;
import com.personalassistant.entity.TaskExecution;
import org.springframework.stereotype.Component;

@Component
public class TaskExecutionMapper {

    public TaskExecutionResponse toResponse(TaskExecution execution) {

        return new TaskExecutionResponse(
                execution.getId(),
                execution.getTask().getId(),
                execution.getStartedAt(),
                execution.getEndedAt(),
                execution.getStatus(),
                execution.getFeedback(),
                execution.getCreatedAt()
        );
    }
}