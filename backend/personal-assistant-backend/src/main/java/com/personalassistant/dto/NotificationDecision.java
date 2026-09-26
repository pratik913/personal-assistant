package com.personalassistant.dto;

public record NotificationDecision(
        boolean shouldNotify,
        String reason
) {

    public static NotificationDecision allow() {

        return new NotificationDecision(
                true,
                "Notification allowed."
        );
    }

    public static NotificationDecision reject(
            String reason
    ) {

        return new NotificationDecision(
                false,
                reason
        );
    }
}