package com.personalassistant.mapper;

import com.personalassistant.dto.CreateUserRequest;
import com.personalassistant.dto.UserProfileResponse;
import com.personalassistant.dto.UserResponse;
import com.personalassistant.entity.User;
import org.springframework.stereotype.Component;

@Component
public class UserMapper {

    /*
     * ==============================
     * CREATE USER
     * ==============================
     *
     * Used during user registration.
     */
    public User toEntity(CreateUserRequest request) {

        User user = new User();

        user.setName(request.getName());
        user.setEmail(request.getEmail());
        user.setTimezone(request.getTimezone());

        return user;
    }


    /*
     * ==============================
     * EXISTING USER RESPONSE
     * ==============================
     *
     * Used by the existing user APIs.
     *
     * Keep this method so we don't
     * break the existing functionality.
     */
    public UserResponse toResponse(User user) {

        UserResponse response = new UserResponse();

        response.setId(user.getId());
        response.setName(user.getName());
        response.setEmail(user.getEmail());
        response.setTimezone(user.getTimezone());
        response.setCreatedAt(user.getCreatedAt());
        response.setUpdatedAt(user.getUpdatedAt());

        return response;
    }


    /*
     * ==============================
     * SETTINGS / PROFILE RESPONSE
     * ==============================
     *
     * Used by:
     *
     * GET /api/users/me
     *
     * This contains the information
     * required by the Settings page.
     */
    public UserProfileResponse toProfileResponse(User user) {

        return new UserProfileResponse(

                user.getId(),

                user.getName(),

                user.getEmail(),

                user.getPhoneNumber(),

                user.isPhoneNumberVerified(),

                user.getTimezone(),

                user.getProfileImageKey() != null
                        && !user.getProfileImageKey().isBlank()

        );
    }
}