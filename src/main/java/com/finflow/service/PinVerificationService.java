package com.finflow.service;

import com.finflow.entity.User;
import com.finflow.exception.InvalidTransferException;
import com.finflow.repository.UserRepository;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

@Service
public class PinVerificationService {
    private static final int MAX_FAILURES = 5;
    private static final int LOCK_MINUTES = 15;

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final SecurityAuditService auditService;

    public PinVerificationService(UserRepository userRepository, PasswordEncoder passwordEncoder,
                                  SecurityAuditService auditService) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.auditService = auditService;
    }

    @Transactional
    public User verify(User principal, String pin) {
        User user = userRepository.findByIdForUpdate(principal.getId())
                .orElseThrow(() -> new InvalidTransferException("Transaction PIN verification failed"));
        LocalDateTime now = LocalDateTime.now();
        if (user.getPinLockedUntil() != null && user.getPinLockedUntil().isAfter(now)) {
            throw new InvalidTransferException("Transaction PIN is temporarily locked. Try again later.");
        }
        if (user.getTransactionPinHash() == null || pin == null ||
                !passwordEncoder.matches(pin, user.getTransactionPinHash())) {
            int failures = user.getFailedPinAttempts() + 1;
            user.setFailedPinAttempts(failures);
            auditService.log(user, "PIN_FAILED", "User/" + user.getId(), null, "MEDIUM", "FAILED",
                    "Transaction PIN verification failed.");
            if (failures >= MAX_FAILURES) {
                user.setPinLockedUntil(now.plusMinutes(LOCK_MINUTES));
                auditService.log(user, "PIN_LOCKED", "User/" + user.getId(), null, "HIGH", "LOCKED",
                        "Transaction PIN locked after repeated failures.");
            }
            userRepository.save(user);
            throw new InvalidTransferException(failures >= MAX_FAILURES
                    ? "Transaction PIN is temporarily locked. Try again later."
                    : "Transaction PIN verification failed");
        }
        user.setFailedPinAttempts(0);
        user.setPinLockedUntil(null);
        userRepository.save(user);
        auditService.log(user, "PIN_VERIFIED", "User/" + user.getId(), null, "LOW", "SUCCESS",
                "Transaction PIN verified.");
        return user;
    }
}
