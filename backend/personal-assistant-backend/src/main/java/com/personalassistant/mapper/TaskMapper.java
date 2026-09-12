package com.personalassistant.mapper;

import com.personalassistant.dto.CreateTaskRequest;
import com.personalassistant.dto.TaskResponse;
import com.personalassistant.entity.Task;
import org.springframework.stereotype.Component;

@Component
public class TaskMapper {
    public  Task toEntity(CreateTaskRequest request){
            Task task = new Task();
            task.setTitle(request.getTitle());
        task.setDescription(request.getDescription());
        task.setStatus(request.getStatus());
        task.setPriority(request.getPriority());
        task.setDueDate(request.getDueDate());
        task.setEstimatedMinutes(request.getEstimatedMinutes());
        return task;
    }

    public   TaskResponse toResponse(Task task){
        TaskResponse taskResponse= new TaskResponse();
        taskResponse.setId(task.getId());
        taskResponse.setTitle(task.getTitle());
        taskResponse.setDescription(task.getDescription());
        taskResponse.setStatus(task.getStatus());
        taskResponse.setPriority(task.getPriority());
        taskResponse.setDueDate(task.getDueDate());
        taskResponse.setEstimatedMinutes(task.getEstimatedMinutes());
        taskResponse.setCreatedAt(task.getCreatedAt());
        taskResponse.setUpdatedAt(task.getUpdatedAt());
        return taskResponse;
    }
}
