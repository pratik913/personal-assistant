package com.personalassistant.mapper;

import com.personalassistant.dto.CreateTaskRequest;
import com.personalassistant.dto.TaskResponse;
import com.personalassistant.dto.UpdateTaskRequest;
import com.personalassistant.entity.Task;
import org.springframework.stereotype.Component;

@Component
public class TaskMapper {

    public Task toEntity(CreateTaskRequest request) {

        Task task = new Task();

        task.setTitle(request.getTitle());
        task.setDescription(request.getDescription());
        task.setStatus(request.getStatus());
        task.setPriority(request.getPriority());
        task.setDueDate(request.getDueDate());
        task.setEstimatedMinutes(request.getEstimatedMinutes());

        return task;
    }

    public TaskResponse toResponse(Task task) {

        TaskResponse response = new TaskResponse();

        response.setId(task.getId());
        response.setTitle(task.getTitle());
        response.setDescription(task.getDescription());
        response.setStatus(task.getStatus());
        response.setPriority(task.getPriority());
        response.setDueDate(task.getDueDate());
        response.setEstimatedMinutes(task.getEstimatedMinutes());
        response.setCreatedAt(task.getCreatedAt());
        response.setUpdatedAt(task.getUpdatedAt());

        if (task.getCapture() != null) {
            response.setCaptureId(task.getCapture().getId());
        }

        return response;
    }
}