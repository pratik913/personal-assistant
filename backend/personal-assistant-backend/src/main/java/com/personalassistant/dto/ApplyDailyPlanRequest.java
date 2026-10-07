package com.personalassistant.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;

public record ApplyDailyPlanRequest(
        @NotNull
        LocalDate planningDate,

        @NotNull
        LocalTime availableFrom,

        @NotNull
        LocalTime availableUntil,

        @NotEmpty
        List<@Valid AiPlanItem> items
) {
}
