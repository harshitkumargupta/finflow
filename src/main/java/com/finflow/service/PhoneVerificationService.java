package com.finflow.service;

import com.finflow.entity.PhoneVerificationToken;
import com.finflow.entity.User;
import com.finflow.exception.TooManyRequestsException;
import com.finflow.phone.PhoneOtpRequested;
import com.finflow.repository.PhoneVerificationTokenRepository;
import com.finflow.repository.UserRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.HexFormat;
import java.util.Optional;

@Service
public class PhoneVerificationService {

    public static final long COOLDOWN_SECONDS = 60;
    public static final int HOURLY_LIMIT = 5;
    private static final SecureRandom RANDOM = new SecureRandom();

    public enum SendOutcome { SENT, ALREADY_VERIFIED }

    public enum VerifyOutcome { VERIFIED, INVALID, EXPIRED, LOCKED, ALREADY_VERIFIED }

    private final PhoneVerificationTokenRepository tokenRepository;
    private final UserRepository userRepository;
    private final ApplicationEventPublisher events;
    private final long ttlMinutes;
    private final int maxAttempts;

    public PhoneVerificationService(PhoneVerificationTokenRepository tokenRepository, UserRepository userRepository,
                                    ApplicationEventPublisher events,
                                    @Value("${finflow.phone.otp-ttl-minutes:10}") long ttlMinutes,
                                    @Value("${finflow.phone.max-attempts:5}") int maxAttempts) {
        this.tokenRepository = tokenRepository;
        this.userRepository = userRepository;
        this.events = events;
        this.ttlMinutes = Math.max(ttlMinutes, 1);
        this.maxAttempts = Math.max(maxAttempts, 1);
    }

    @Transactional
    public SendOutcome send(User user) {
        if (user.isPhoneVerified()) return SendOutcome.ALREADY_VERIFIED;
        LocalDateTime now = LocalDateTime.now();
        Optional<PhoneVerificationToken> last = tokenRepository.findFirstByUserIdOrderByCreatedAtDesc(user.getId());
        if (last.isPresent()) {
            long since = Duration.between(last.get().getCreatedAt(), now).getSeconds();
            if (since < COOLDOWN_SECONDS) {
                long wait = COOLDOWN_SECONDS - since;
                throw new TooManyRequestsException("Please wait " + wait + " seconds before requesting another code.", wait);
            }
        }
        if (tokenRepository.countByUserIdAndCreatedAtAfter(user.getId(), now.minusHours(1)) >= HOURLY_LIMIT) {
            throw new TooManyRequestsException("Too many codes requested this hour. Try again later.", 3600);
        }
        String code = issue(user, now);
        events.publishEvent(new PhoneOtpRequested(user.getId(), code, ttlMinutes));
        return SendOutcome.SENT;
    }

    @Transactional
    public VerifyOutcome verify(User user, String rawCode) {
        if (user.isPhoneVerified()) return VerifyOutcome.ALREADY_VERIFIED;
        if (rawCode == null) return VerifyOutcome.INVALID;
        String normalized = rawCode.trim();
        if (!normalized.matches("\\d{6}")) return VerifyOutcome.INVALID;
        LocalDateTime now = LocalDateTime.now();
        Optional<PhoneVerificationToken> found = tokenRepository.findFirstByUserIdOrderByCreatedAtDesc(user.getId());
        if (found.isEmpty()) return VerifyOutcome.EXPIRED;
        PhoneVerificationToken token = found.get();
        if (token.getUsedAt() != null) return VerifyOutcome.LOCKED;
        if (token.getExpiresAt().isBefore(now)) return VerifyOutcome.EXPIRED;
        if (!MessageDigest.isEqual(hash(normalized).getBytes(StandardCharsets.UTF_8),
                token.getCodeHash().getBytes(StandardCharsets.UTF_8))) {
            token.setAttempts(token.getAttempts() + 1);
            if (token.getAttempts() >= maxAttempts) {
                token.setUsedAt(now);
                tokenRepository.save(token);
                return VerifyOutcome.LOCKED;
            }
            tokenRepository.save(token);
            return VerifyOutcome.INVALID;
        }
        User managed = userRepository.findById(user.getId()).orElse(user);
        managed.setPhoneVerified(true);
        managed.setPhoneVerifiedAt(now);
        userRepository.save(managed);
        tokenRepository.invalidateOpenTokens(managed.getId(), now);
        return VerifyOutcome.VERIFIED;
    }

    private String issue(User user, LocalDateTime now) {
        tokenRepository.invalidateOpenTokens(user.getId(), now);
        String code = String.format("%06d", RANDOM.nextInt(1_000_000));
        tokenRepository.save(new PhoneVerificationToken(user, hash(code), now.plusMinutes(ttlMinutes), now));
        return code;
    }

    public static String hash(String raw) {
        try {
            return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(raw.getBytes(StandardCharsets.UTF_8)));
        } catch (NoSuchAlgorithmException exception) {
            throw new IllegalStateException(exception);
        }
    }
}
