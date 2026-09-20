package com.personalassistant.dto;

import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDate;
import java.time.LocalTime;

@Getter
@Setter
public class CreateAiPlanRequest {

    @NotNull
    private LocalDate planningDate;

    @NotNull
    private LocalTime availableFrom;

    @NotNull
    private LocalTime availableUntil;
}