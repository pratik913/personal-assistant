package com.personalassistant.dto;

import java.time.LocalDate;
import java.time.LocalTime;

public record DailyPlannerResponse(
        LocalDate planningDate,
        LocalTime availableFrom,
        LocalTime availableUntil,
        AiPlanResponse plan
) {
}