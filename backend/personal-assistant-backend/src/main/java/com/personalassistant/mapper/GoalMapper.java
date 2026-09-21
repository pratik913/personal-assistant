package com.personalassistant.mapper;

import com.personalassistant.dto.GoalResponse;
import com.personalassistant.entity.Goal;
import org.springframework.stereotype.Component;

@Component
public class GoalMapper {

    public GoalResponse toResponse(Goal goal) {
        return new GoalResponse(
                goal.getId(),
                goal.getTitle(),
                goal.getDescription(),
                goal.getTargetDate(),
                goal.getCreatedAt(),
                goal.getUpdatedAt()
        );
    }
}