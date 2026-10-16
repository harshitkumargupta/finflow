package com.finflow.service;

import com.finflow.dto.CreateUserRequest;
import com.finflow.dto.UserResponse;
import com.finflow.entity.User;
import com.finflow.exception.ResourceAlreadyExistsException;
import com.finflow.repository.UserRepository;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import com.finflow.exception.ResourceAlreadyExistsException;

import java.time.LocalDateTime;

@Service
public class UserService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    public UserService(
            UserRepository userRepository,
            PasswordEncoder passwordEncoder) {

        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
    }

    public UserResponse createUser(CreateUserRequest request) {

        if (userRepository.existsByEmail(request.getEmail())) {
            throw new ResourceAlreadyExistsException("Email already registered");
        }

        if (userRepository.existsByPhone(request.getPhone())) {
            throw new ResourceAlreadyExistsException("Phone already registered");
        }

        User user = new User();

        user.setName(request.getName());
        user.setEmail(request.getEmail());
        user.setPhone(request.getPhone());

        // Hash the password before storing it
        user.setPassword(passwordEncoder.encode(request.getPassword()));

        user.setCreatedAt(LocalDateTime.now());

        User savedUser = userRepository.save(user);

        return new UserResponse(
                savedUser.getId(),
                savedUser.getName(),
                savedUser.getEmail(),
                savedUser.getPhone(),
                savedUser.getCreatedAt(),
                savedUser.isEmailVerified()
        );
    }

    public UserResponse getCurrentUser() {
        Authentication authentication =
                SecurityContextHolder.getContext().getAuthentication();
        User user = userRepository.findByEmail(authentication.getName())
                .orElseThrow(() ->
                        new AccessDeniedException("Authenticated user not found"));

        return new UserResponse(
                user.getId(),
                user.getName(),
                user.getEmail(),
                user.getPhone(),
                user.getCreatedAt(),
                user.isEmailVerified(),
                user.isPhoneVerified()
        );
    }
}