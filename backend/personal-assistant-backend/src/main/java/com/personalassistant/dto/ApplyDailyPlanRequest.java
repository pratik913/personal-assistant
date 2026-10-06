package com.personalassistant.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;

import java.util.List;

public record ApplyDailyPlanRequest(

        @NotEmpty
        List<@Valid AiPlanItem> items
) {
}