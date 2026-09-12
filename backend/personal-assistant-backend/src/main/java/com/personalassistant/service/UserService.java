package com.personalassistant.service;

import com.personalassistant.dto.CreateUserRequest;
import com.personalassistant.dto.UserResponse;
import com.personalassistant.entity.User;
import com.personalassistant.exception.EmailAlreadyExistsException;
import com.personalassistant.mapper.UserMapper;
import com.personalassistant.repository.UserRepository;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.UUID;

@Service
public class UserService {

    private final UserRepository userRepository;
    private final UserMapper userMapper;
    private final PasswordEncoder passwordEncoder;

    public UserService(
            UserRepository userRepository,
            UserMapper userMapper,
            PasswordEncoder passwordEncoder
    ) {
        this.userRepository = userRepository;
        this.userMapper = userMapper;
        this.passwordEncoder = passwordEncoder;
    }

    public UserResponse createUser(CreateUserRequest request) {

        if (userRepository.findByEmail(request.getEmail()).isPresent()) {
            throw new EmailAlreadyExistsException("Email already exists");
        }

        User user = userMapper.toEntity(request);

        String passwordHash = passwordEncoder.encode(request.getPassword());

        user.setPasswordHash(passwordHash);

        User savedUser = userRepository.save(user);

        return userMapper.toResponse(savedUser);
    }

    public UserResponse getCurrentUser(String userId) {

        User user = userRepository.findById(UUID.fromString(userId))
                .orElseThrow(() ->
                        new RuntimeException("User not found")
                );

        return userMapper.toResponse(user);
    }
}