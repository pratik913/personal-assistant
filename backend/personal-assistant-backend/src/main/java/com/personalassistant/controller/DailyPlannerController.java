package com.personalassistant.controller;

import com.personalassistant.dto.DailyPlannerResponse;
import com.personalassistant.service.DailyPlannerService;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import org.springframework.security.core.Authentication;

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
        this.dailyPlannerService = dailyPlannerService;
    }

    @PostMapping("/generate")
    @ResponseStatus(HttpStatus.CREATED)
    public DailyPlannerResponse generatePlan(
            @RequestParam LocalDate planningDate,
            @RequestParam LocalTime availableFrom,
            @RequestParam LocalTime availableUntil,
            Authentication authentication
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