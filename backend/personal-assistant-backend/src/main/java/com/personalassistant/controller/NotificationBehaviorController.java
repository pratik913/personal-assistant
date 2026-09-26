package com.personalassistant.controller;

import com.personalassistant.dto.NotificationBehaviorInsightResponse;
import com.personalassistant.service.NotificationBehaviorService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

@RestController
@RequestMapping("/api/notification-insights")
@RequiredArgsConstructor
public class NotificationBehaviorController {

    private final NotificationBehaviorService notificationBehaviorService;

    @GetMapping
    public ResponseEntity<NotificationBehaviorInsightResponse> getInsights(
            Authentication authentication
    ) {

        UUID userId =
                UUID.fromString(authentication.getName());

        return ResponseEntity.ok(
                notificationBehaviorService.getInsights(userId)
        );
    }
}