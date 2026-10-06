package com.personalassistant.dto;

import jakarta.validation.constraints.NotNull;

import java.time.LocalDate;
import java.time.LocalTime;

public record CreateDailyPlanRequest(

        @NotNull
        LocalDate planningDate,

        @NotNull
        LocalTime availableFrom,

        @NotNull
        LocalTime availableUntil
) {
}