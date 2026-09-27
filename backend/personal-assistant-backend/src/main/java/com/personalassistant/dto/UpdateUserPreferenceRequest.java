package com.personalassistant.dto;

import jakarta.validation.constraints.Pattern;

public record UpdateUserPreferenceRequest(

        @Pattern(
                regexp = "light|dark",
                message = "Theme must be either light or dark"
        )
        String theme,

        @Pattern(
                regexp = "balanced|focused|flexible",
                message = "Planning style must be balanced, focused, or flexible"
        )
        String planningStyle
) {
}