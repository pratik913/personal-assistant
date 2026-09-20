package com.personalassistant.mapper;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.personalassistant.dto.AiPlanData;
import com.personalassistant.dto.AiPlanResponse;
import com.personalassistant.entity.AiPlan;
import org.springframework.stereotype.Component;

@Component
public class AiPlanMapper {

    private final ObjectMapper objectMapper;

    public AiPlanMapper(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    public AiPlanResponse toResponse(
            AiPlan aiPlan
    ) {
        try {
            AiPlanData planData =
                    objectMapper.readValue(
                            aiPlan.getPlanData(),
                            AiPlanData.class
                    );

            return new AiPlanResponse(
                    aiPlan.getId(),
                    aiPlan.getSummary(),
                    planData,
                    aiPlan.getCreatedAt(),
                    aiPlan.getUpdatedAt()
            );

        } catch (JsonProcessingException exception) {
            throw new IllegalStateException(
                    "Unable to read AI plan data.",
                    exception
            );
        }
    }
}