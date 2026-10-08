package com.personalassistant.controller;

import com.personalassistant.dto.NotificationBehaviorInsightResponse;
import com.personalassistant.dto.NotificationTimingRecommendationResponse;
import com.personalassistant.service.NotificationBehaviorService;
import com.personalassistant.service.NotificationTimingRecommendationService;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

@RestController
@RequestMapping("/api/notification-insights")
public class NotificationBehaviorController {

    private final NotificationBehaviorService notificationBehaviorService;

    private final NotificationTimingRecommendationService
            notificationTimingRecommendationService;

    public NotificationBehaviorController(
            NotificationBehaviorService notificationBehaviorService,
            NotificationTimingRecommendationService
                    notificationTimingRecommendationService
    ) {
        this.notificationBehaviorService =
                notificationBehaviorService;

        this.notificationTimingRecommendationService =
                notificationTimingRecommendationService;
    }

    @GetMapping
    public NotificationBehaviorInsightResponse getInsights(
            Authentication authentication
    ) {

        UUID userId =
                UUID.fromString(
                        authentication.getName()
                );

        return notificationBehaviorService.getInsights(
                userId
        );
    }

    @GetMapping("/recommendation")
    public NotificationTimingRecommendationResponse
    getTimingRecommendation(
            Authentication authentication
    ) {

        UUID userId =
                UUID.fromString(
                        authentication.getName()
                );

        return notificationTimingRecommendationService
                .getRecommendation(userId);
    }
}