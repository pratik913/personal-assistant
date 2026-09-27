package com.personalassistant.service;

import com.personalassistant.dto.UpdateUserPreferenceRequest;
import com.personalassistant.dto.UserPreferenceResponse;
import com.personalassistant.entity.User;
import com.personalassistant.entity.UserPreference;
import com.personalassistant.exception.UserNotFoundException;
import com.personalassistant.mapper.UserPreferenceMapper;
import com.personalassistant.repository.UserPreferenceRepository;
import com.personalassistant.repository.UserRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
@Transactional
public class UserPreferenceService {

    private final UserPreferenceRepository preferenceRepository;

    private final UserRepository userRepository;

    private final UserPreferenceMapper mapper;

    public UserPreferenceService(
            UserPreferenceRepository preferenceRepository,
            UserRepository userRepository,
            UserPreferenceMapper mapper
    ) {

        this.preferenceRepository = preferenceRepository;
        this.userRepository = userRepository;
        this.mapper = mapper;
    }

    public UserPreferenceResponse getPreferences(
            UUID userId
    ) {

        UserPreference preference =
                getOrCreate(userId);

        return mapper.toResponse(preference);
    }

    public UserPreferenceResponse updatePreferences(
            UUID userId,
            UpdateUserPreferenceRequest request
    ) {

        UserPreference preference =
                getOrCreate(userId);

        if (request.theme() != null) {

            preference.setTheme(
                    request.theme()
            );
        }

        if (request.planningStyle() != null) {

            preference.setPlanningStyle(
                    request.planningStyle()
            );
        }

        return mapper.toResponse(
                preferenceRepository.save(
                        preference
                )
        );
    }

    private UserPreference getOrCreate(
            UUID userId
    ) {

        return preferenceRepository
                .findByUserId(userId)
                .orElseGet(() -> {

                    User user =
                            userRepository
                                    .findById(userId)
                                    .orElseThrow(() ->
                                            new UserNotFoundException(
                                                    "User not found."
                                            )
                                    );

                    UserPreference preference =
                            new UserPreference();

                    preference.setUser(user);
                    preference.setTheme("light");
                    preference.setPlanningStyle(
                            "balanced"
                    );

                    return preferenceRepository.save(
                            preference
                    );
                });
    }
}