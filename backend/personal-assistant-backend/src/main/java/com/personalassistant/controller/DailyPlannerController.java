package com.personalassistant.controller;

import com.personalassistant.dto.DailyPlannerResponse;
import com.personalassistant.service.DailyPlannerService;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.UUID;

@RestController
@RequestMapping("/api/daily-planner")
public class DailyPlannerController {

    private final DailyPlannerService dailyPlannerService;

    public DailyPlannerController(
            DailyPlannerService dailyPlannerService
    ) {
        this.dailyPlannerService =
                dailyPlannerService;
    }

    @PostMapping("/generate")
    @ResponseStatus(HttpStatus.CREATED)
    public DailyPlannerResponse generatePlan(
            @RequestParam LocalDate planningDate,
            @RequestParam LocalTime availableFrom,
            @RequestParam LocalTime availableUntil,
            org.springframework.security.core.Authentication authentication
    ) {

        UUID userId =
                UUID.fromString(
                        authentication.getName()
                );

        return dailyPlannerService.generateDailyPlan(
                userId,
                planningDate,
                availableFrom,
                availableUntil
        );
    }
}