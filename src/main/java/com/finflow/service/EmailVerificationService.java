package com.finflow.service;

import com.finflow.email.VerificationEmailRequested;
import com.finflow.entity.EmailVerificationToken;
import com.finflow.entity.User;
import com.finflow.exception.TooManyRequestsException;
import com.finflow.repository.EmailVerificationTokenRepository;
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
import java.util.Base64;
import java.util.HexFormat;
import java.util.Optional;

@Service
public class EmailVerificationService {

    public static final long COOLDOWN_SECONDS = 60;
    public static final int HOURLY_LIMIT = 5;
    private static final SecureRandom RANDOM = new SecureRandom();

    public enum ResendOutcome { SENT, ALREADY_VERIFIED }

    private final EmailVerificationTokenRepository tokenRepository;
    private final UserRepository userRepository;
    private final ApplicationEventPublisher events;
    private final long ttlHours;

    public EmailVerificationService(EmailVerificationTokenRepository tokenRepository, UserRepository userRepository,
                                    ApplicationEventPublisher events,
                                    @Value("${finflow.mail.verification-ttl-hours:24}") long ttlHours) {
        this.tokenRepository = tokenRepository;
        this.userRepository = userRepository;
        this.events = events;
        this.ttlHours = ttlHours;
    }

    @Transactional
    public void issueAndSend(User user) {
        String raw = issue(user);
        events.publishEvent(new VerificationEmailRequested(user.getEmail(), user.getName(), raw, ttlHours));
    }

    @Transactional
    public ResendOutcome resend(User user) {
        if (user.isEmailVerified()) return ResendOutcome.ALREADY_VERIFIED;
        LocalDateTime now = LocalDateTime.now();
        Optional<EmailVerificationToken> last = tokenRepository.findFirstByUserIdOrderByCreatedAtDesc(user.getId());
        if (last.isPresent()) {
            long since = Duration.between(last.get().getCreatedAt(), now).getSeconds();
            if (since < COOLDOWN_SECONDS) {
                long wait = COOLDOWN_SECONDS - since;
                throw new TooManyRequestsException("Please wait " + wait + " seconds before requesting another email.", wait);
            }
        }
        if (tokenRepository.countByUserIdAndCreatedAtAfter(user.getId(), now.minusHours(1)) >= HOURLY_LIMIT) {
            throw new TooManyRequestsException("Too many verification emails this hour. Try again later.", 3600);
        }
        issueAndSend(user);
        return ResendOutcome.SENT;
    }

    @Transactional
    public boolean verify(String rawToken) {
        if (rawToken == null || rawToken.isBlank() || rawToken.length() > 128) return false;
        LocalDateTime now = LocalDateTime.now();
        Optional<EmailVerificationToken> found = tokenRepository.findByTokenHash(hash(rawToken.trim()));
        if (found.isEmpty()) return false;
        EmailVerificationToken token = found.get();
        if (token.getUsedAt() != null || token.getExpiresAt().isBefore(now)) return false;
        User user = token.getUser();
        user.setEmailVerified(true);
        user.setEmailVerifiedAt(now);
        userRepository.save(user);
        tokenRepository.invalidateOpenTokens(user.getId(), now);
        return true;
    }

    String issue(User user) {
        LocalDateTime now = LocalDateTime.now();
        tokenRepository.invalidateOpenTokens(user.getId(), now);
        byte[] bytes = new byte[32];
        RANDOM.nextBytes(bytes);
        String raw = Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
        tokenRepository.save(new EmailVerificationToken(user, hash(raw), now.plusHours(ttlHours), now));
        return raw;
    }

    public static String hash(String raw) {
        try {
            return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(raw.getBytes(StandardCharsets.UTF_8)));
        } catch (NoSuchAlgorithmException exception) {
            throw new IllegalStateException(exception);
        }
    }
}
