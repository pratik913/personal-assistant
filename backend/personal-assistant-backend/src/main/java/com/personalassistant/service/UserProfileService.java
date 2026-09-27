package com.personalassistant.service;

import com.personalassistant.dto.UpdateUserProfileRequest;
import com.personalassistant.dto.UserProfileResponse;
import com.personalassistant.entity.User;
import com.personalassistant.exception.UserNotFoundException;
import com.personalassistant.mapper.UserMapper;
import com.personalassistant.repository.UserRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.util.Set;
import java.util.UUID;

@Service
@Transactional
public class UserProfileService {

    private static final long MAX_PROFILE_IMAGE_SIZE =
            5 * 1024 * 1024;

    private static final Set<String> ALLOWED_IMAGE_TYPES =
            Set.of(
                    "image/jpeg",
                    "image/png",
                    "image/webp"
            );

    private final UserRepository userRepository;

    private final UserMapper userMapper;

    private final S3StorageService s3StorageService;

    public UserProfileService(
            UserRepository userRepository,
            UserMapper userMapper,
            S3StorageService s3StorageService
    ) {
        this.userRepository = userRepository;
        this.userMapper = userMapper;
        this.s3StorageService = s3StorageService;
    }

    @Transactional(readOnly = true)
    public UserProfileResponse getProfile(
            UUID userId
    ) {

        User user = getUser(userId);

        return userMapper.toProfileResponse(user);
    }

    public UserProfileResponse updateProfile(
            UUID userId,
            UpdateUserProfileRequest request
    ) {

        User user = getUser(userId);

        if (
                !request.email()
                        .equalsIgnoreCase(user.getEmail()) &&
                        userRepository.existsByEmailAndIdNot(
                                request.email(),
                                userId
                        )
        ) {

            throw new IllegalArgumentException(
                    "Email is already registered."
            );
        }

        String previousPhoneNumber =
                user.getPhoneNumber();

        String phoneNumber =
                normalizePhoneNumber(
                        request.phoneNumber()
                );

        user.setName(
                request.name().trim()
        );

        user.setEmail(
                request.email().trim().toLowerCase()
        );

        user.setPhoneNumber(
                phoneNumber
        );

        user.setTimezone(
                request.timezone().trim()
        );

        /*
         * Compare the previous value with the new value
         * BEFORE replacing the entity field.
         *
         * This prevents a phone-number change from
         * incorrectly keeping the old verification state.
         */
        if (
                !sameValue(
                        previousPhoneNumber,
                        phoneNumber
                )
        ) {

            user.setPhoneNumberVerified(
                    false
            );
        }

        return userMapper.toProfileResponse(
                userRepository.save(user)
        );
    }

    public UserProfileResponse uploadProfileImage(
            UUID userId,
            MultipartFile file
    ) {

        User user = getUser(userId);

        validateProfileImage(file);

        String oldKey =
                user.getProfileImageKey();

        String extension =
                getExtension(
                        file.getOriginalFilename()
                );

        String key =
                "profile-images/"
                        + userId
                        + "/profile"
                        + extension;

        s3StorageService.upload(
                key,
                file
        );

        user.setProfileImageKey(
                key
        );

        userRepository.save(user);

        if (
                oldKey != null &&
                        !oldKey.equals(key)
        ) {

            try {

                s3StorageService.delete(
                        oldKey
                );

            } catch (Exception ignored) {

                /*
                 * The new image is already saved.
                 * Old-object cleanup can be retried
                 * later without failing the profile update.
                 */
            }
        }

        return userMapper.toProfileResponse(
                user
        );
    }

    private User getUser(
            UUID userId
    ) {

        return userRepository
                .findById(userId)
                .orElseThrow(() ->
                        new UserNotFoundException(
                                "User not found."
                        )
                );
    }

    private void validateProfileImage(
            MultipartFile file
    ) {

        if (
                file == null ||
                        file.isEmpty()
        ) {

            throw new IllegalArgumentException(
                    "Profile image is required."
            );
        }

        if (
                file.getSize()
                        > MAX_PROFILE_IMAGE_SIZE
        ) {

            throw new IllegalArgumentException(
                    "Profile image must not exceed 5 MB."
            );
        }

        String contentType =
                file.getContentType();

        if (
                contentType == null ||
                        !ALLOWED_IMAGE_TYPES.contains(
                                contentType
                        )
        ) {

            throw new IllegalArgumentException(
                    "Only JPG, PNG, and WebP images are supported."
            );
        }
    }

    private String normalizePhoneNumber(
            String phoneNumber
    ) {

        if (
                phoneNumber == null ||
                        phoneNumber.isBlank()
        ) {

            return null;
        }

        return phoneNumber.trim();
    }

    private boolean sameValue(
            String first,
            String second
    ) {

        if (first == null) {
            return second == null;
        }

        return first.equals(second);
    }

    private String getExtension(
            String filename
    ) {

        if (filename == null) {
            return ".jpg";
        }

        String lowerCase =
                filename.toLowerCase();

        if (lowerCase.endsWith(".png")) {
            return ".png";
        }

        if (lowerCase.endsWith(".webp")) {
            return ".webp";
        }

        return ".jpg";
    }
}