package com.personalassistant.dto;

import java.time.LocalDate;
import java.util.List;

public record AiPlanData(
        LocalDate planningDate,
        List<AiPlanItem> items
) {}