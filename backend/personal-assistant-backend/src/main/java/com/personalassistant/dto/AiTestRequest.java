package com.personalassistant.dto;

import jakarta.validation.constraints.NotBlank;

public record AiTestRequest(
        @NotBlank
        String content
) {
}