package com.personalassistant.controller;

import com.personalassistant.dto.AiPlanData;
import com.personalassistant.dto.ApplyDailyPlanRequest;
import com.personalassistant.dto.CreateDailyPlanRequest;
import com.personalassistant.dto.ScheduleEntryResponse;
import com.personalassistant.service.DailyPlannerService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/planner")
public class DailyPlannerController {

    private final DailyPlannerService dailyPlannerService;

    public DailyPlannerController(
            DailyPlannerService dailyPlannerService
    ) {
        this.dailyPlannerService =
                dailyPlannerService;
    }

    @PostMapping("/daily")
    public ResponseEntity<AiPlanData> generateDailyPlan(
            @Valid @RequestBody CreateDailyPlanRequest request,
            Authentication authentication
    ) {

        UUID userId =
                UUID.fromString(
                        authentication.getName()
                );

        AiPlanData plan =
                dailyPlannerService
                        .generateDailyPlan(
                                userId,
                                request
                        );

        return ResponseEntity.ok(
                plan
        );
    }

    @PostMapping("/daily/apply")
    public ResponseEntity<List<ScheduleEntryResponse>> applyDailyPlan(
            @Valid @RequestBody ApplyDailyPlanRequest request,
            Authentication authentication
    ) {

        UUID userId =
                UUID.fromString(
                        authentication.getName()
                );

        List<ScheduleEntryResponse> entries =
                dailyPlannerService
                        .applyDailyPlan(
                                userId,
                                request
                        );

        return ResponseEntity.ok(
                entries
        );
    }
}