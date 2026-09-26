package com.personalassistant.service;

import com.personalassistant.dto.AiPlanResponse;
import com.personalassistant.dto.CreateAiPlanRequest;
import com.personalassistant.dto.DailyPlannerResponse;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.UUID;

@Service
@Transactional
public class DailyPlannerService {

    private final AiPlannerService aiPlannerService;

    public DailyPlannerService(
            AiPlannerService aiPlannerService
    ) {
        this.aiPlannerService = aiPlannerService;
    }

    public DailyPlannerResponse generateDailyPlan(
            UUID userId,
            LocalDate planningDate,
            LocalTime availableFrom,
            LocalTime availableUntil
    ) {

        if (planningDate == null) {
            throw new IllegalArgumentException(
                    "Planning date is required."
            );
        }

        if (availableFrom == null || availableUntil == null) {
            throw new IllegalArgumentException(
                    "Available start and end times are required."
            );
        }

        if (!availableFrom.isBefore(availableUntil)) {
            throw new IllegalArgumentException(
                    "Available start time must be before available end time."
            );
        }

        CreateAiPlanRequest request =
                new CreateAiPlanRequest(
                        planningDate,
                        availableFrom,
                        availableUntil
                );

        AiPlanResponse plan =
                aiPlannerService.createPlan(
                        userId,
                        request
                );

        return new DailyPlannerResponse(
                planningDate,
                availableFrom,
                availableUntil,
                plan
        );
    }
}