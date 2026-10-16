package com.finflow.service;

import com.finflow.dto.AuthResponse;
import com.finflow.dto.LoginRequest;
import com.finflow.dto.RegisterRequest;
import com.finflow.entity.User;
import com.finflow.exception.ResourceAlreadyExistsException;
import com.finflow.repository.UserRepository;
import com.finflow.security.JwtService;

import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

@Service
public class AuthService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final AuthenticationManager authenticationManager;
    private final JwtService jwtService;
    private final SecurityAuditService auditService;
    private final EmailVerificationService emailVerificationService;

    public AuthService(
            UserRepository userRepository,
            PasswordEncoder passwordEncoder,
            AuthenticationManager authenticationManager,
            JwtService jwtService,
            SecurityAuditService auditService,
            EmailVerificationService emailVerificationService) {

        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.authenticationManager = authenticationManager;
        this.jwtService = jwtService;
        this.auditService = auditService;
        this.emailVerificationService = emailVerificationService;
    }

    @Transactional
    public void register(RegisterRequest request) {

        String email = request.getEmail().trim();

        if (userRepository.existsByEmail(email)) {
            throw new ResourceAlreadyExistsException("Email already registered");
        }

        if (userRepository.existsByPhone(request.getPhone())) {
            throw new ResourceAlreadyExistsException("Phone already registered");
        }

        // Build the entity explicitly so privileged fields can never be bound from the request.
        User user = new User(
                request.getName().trim(),
                email,
                request.getPhone(),
                passwordEncoder.encode(request.getPassword()),
                LocalDateTime.now()
        );
        user.setRole("USER");
        user.setTransactionPinHash(null);
        user.setFailedPinAttempts(0);
        user.setPinLockedUntil(null);
        user.setEmailVerified(false);

        User saved = userRepository.save(user);
        auditService.log(saved, "REGISTER_SUCCESS", "User", String.valueOf(saved.getId()), "LOW", "SUCCESS", "New user registered");
        emailVerificationService.issueAndSend(saved);
    }

    public AuthResponse login(LoginRequest request) {
        try {
            authenticationManager.authenticate(
                    new UsernamePasswordAuthenticationToken(
                            request.getEmail(),
                            request.getPassword()
                    )
            );

            User user = userRepository.findByEmail(request.getEmail()).orElse(null);
            if (user != null) {
                auditService.log(user, "LOGIN_SUCCESS", "Auth", request.getEmail(), "LOW", "SUCCESS", "User logged in successfully");
            }

            String token = jwtService.generateToken(request.getEmail());
            return new AuthResponse(token);
        } catch (Exception e) {
            userRepository.findByEmail(request.getEmail()).ifPresent(user ->
                    auditService.log(user, "LOGIN_FAILED", "Auth", request.getEmail(), "MEDIUM", "FAILED", "Failed login attempt")
            );
            throw e;
        }
    }
}