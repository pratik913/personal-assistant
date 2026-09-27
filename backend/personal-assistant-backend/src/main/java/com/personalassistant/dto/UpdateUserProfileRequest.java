package com.personalassistant.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record UpdateUserProfileRequest(

        @NotBlank(message = "Name is required")
        @Size(
                max = 100,
                message = "Name must not exceed 100 characters"
        )
        String name,

        @NotBlank(message = "Email is required")
        @Email(message = "Email must be valid")
        @Size(
                max = 255,
                message = "Email must not exceed 255 characters"
        )
        String email,

        @Pattern(
                regexp = "^$|^\\+[1-9]\\d{7,14}$",
                message = "Phone number must use E.164 format"
        )
        String phoneNumber,

        @NotBlank(message = "Timezone is required")
        @Size(
                max = 100,
                message = "Timezone must not exceed 100 characters"
        )
        String timezone
) {
}