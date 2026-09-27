package com.personalassistant.controller;

import com.personalassistant.dto.UpdateUserProfileRequest;
import com.personalassistant.dto.UserProfileResponse;
import com.personalassistant.entity.User;
import com.personalassistant.repository.UserRepository;
import com.personalassistant.service.S3StorageService;
import com.personalassistant.service.UserProfileService;
import jakarta.validation.Valid;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.InputStream;
import java.util.UUID;

@RestController
@RequestMapping("/api/users/me")
public class UserProfileController {

    private final UserProfileService userProfileService;
    private final UserRepository userRepository;
    private final S3StorageService s3StorageService;

    public UserProfileController(
            UserProfileService userProfileService,
            UserRepository userRepository,
            S3StorageService s3StorageService
    ) {
        this.userProfileService = userProfileService;
        this.userRepository = userRepository;
        this.s3StorageService = s3StorageService;
    }

    @PatchMapping
    public UserProfileResponse updateProfile(
            Authentication authentication,
            @Valid @RequestBody UpdateUserProfileRequest request
    ) {
        return userProfileService.updateProfile(
                getUserId(authentication),
                request
        );
    }

    @PostMapping(
            value = "/profile-image",
            consumes = MediaType.MULTIPART_FORM_DATA_VALUE
    )
    public UserProfileResponse uploadProfileImage(
            Authentication authentication,
            @RequestParam("file") MultipartFile file
    ) {
        return userProfileService.uploadProfileImage(
                getUserId(authentication),
                file
        );
    }

    @GetMapping("/profile-image")
    public ResponseEntity<byte[]> getProfileImage(
            Authentication authentication
    ) {
        UUID userId = getUserId(authentication);

        User user = userRepository
                .findById(userId)
                .orElseThrow(() ->
                        new IllegalArgumentException("User not found.")
                );

        if (user.getProfileImageKey() == null) {
            return ResponseEntity
                    .notFound()
                    .build();
        }

        try (
                InputStream inputStream =
                        s3StorageService.download(
                                user.getProfileImageKey()
                        )
        ) {
            byte[] image = inputStream.readAllBytes();

            return ResponseEntity.ok()
                    .contentType(
                            detectContentType(
                                    user.getProfileImageKey()
                            )
                    )
                    .header(
                            HttpHeaders.CACHE_CONTROL,
                            "private, max-age=300"
                    )
                    .body(image);

        } catch (Exception exception) {
            throw new IllegalStateException(
                    "Unable to retrieve profile image.",
                    exception
            );
        }
    }

    private UUID getUserId(Authentication authentication) {
        return UUID.fromString(
                authentication.getName()
        );
    }

    private MediaType detectContentType(String key) {
        String lowerCase = key.toLowerCase();

        if (lowerCase.endsWith(".png")) {
            return MediaType.IMAGE_PNG;
        }

        if (lowerCase.endsWith(".webp")) {
            return MediaType.parseMediaType("image/webp");
        }

        return MediaType.IMAGE_JPEG;
    }
}