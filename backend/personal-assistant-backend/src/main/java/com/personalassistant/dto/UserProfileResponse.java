package com.personalassistant.dto;

import java.util.UUID;

public record UserProfileResponse(

        UUID id,

        String name,

        String email,

        String phoneNumber,

        boolean phoneNumberVerified,

        String timezone,

        boolean hasProfileImage
) {
}