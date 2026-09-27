package com.personalassistant.mapper;

import com.personalassistant.dto.UserPreferenceResponse;
import com.personalassistant.entity.UserPreference;
import org.springframework.stereotype.Component;

@Component
public class UserPreferenceMapper {

    public UserPreferenceResponse toResponse(
            UserPreference preference
    ) {

        return new UserPreferenceResponse(
                preference.getTheme(),
                preference.getPlanningStyle()
        );
    }
}