package com.finflow;

import com.finflow.email.EmailMessage;
import com.finflow.email.EmailProvider;
import com.finflow.email.EmailService;
import com.finflow.email.VerificationEmailRequested;
import com.finflow.entity.EmailVerificationToken;
import com.finflow.entity.User;
import com.finflow.exception.TooManyRequestsException;
import com.finflow.repository.EmailVerificationTokenRepository;
import com.finflow.repository.UserRepository;
import com.finflow.service.EmailVerificationService;
import com.google.zxing.BinaryBitmap;
import com.google.zxing.client.j2se.BufferedImageLuminanceSource;
import com.google.zxing.common.HybridBinarizer;
import com.google.zxing.qrcode.QRCodeReader;
import org.junit.jupiter.api.Test;

import javax.imageio.ImageIO;
import java.io.ByteArrayInputStream;
import java.lang.reflect.Proxy;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

class EmailVerificationTest {

    private final List<EmailVerificationToken> tokens = new ArrayList<>();
    private final List<Object> events = new ArrayList<>();

    @Test
    void tokenIsStoredHashedAndVerifiesOnce() {
        User user = user();
        EmailVerificationService service = service(24);
        service.issueAndSend(user);

        String raw = ((VerificationEmailRequested) events.getFirst()).rawToken();
        assertEquals(1, tokens.size());
        assertNotEquals(raw, tokens.getFirst().getTokenHash());
        assertEquals(EmailVerificationService.hash(raw), tokens.getFirst().getTokenHash());

        assertTrue(service.verify(raw));
        assertTrue(user.isEmailVerified());
        assertNotNull(user.getEmailVerifiedAt());
        assertFalse(service.verify(raw));
        assertFalse(service.verify("not-a-real-token"));
    }

    @Test
    void expiredTokenIsRejected() {
        User user = user();
        EmailVerificationService service = service(-1);
        service.issueAndSend(user);
        String raw = ((VerificationEmailRequested) events.getFirst()).rawToken();
        assertFalse(service.verify(raw));
        assertFalse(user.isEmailVerified());
    }

    @Test
    void resendIsRateLimitedAndSkipsVerifiedUsers() {
        User user = user();
        EmailVerificationService service = service(24);
        service.issueAndSend(user);
        TooManyRequestsException limited = assertThrows(TooManyRequestsException.class, () -> service.resend(user));
        assertTrue(limited.getRetryAfterSeconds() > 0);

        user.setEmailVerified(true);
        assertEquals(EmailVerificationService.ResendOutcome.ALREADY_VERIFIED, service.resend(user));
    }

    @Test
    void reissuingInvalidatesEarlierLinks() {
        User user = user();
        EmailVerificationService service = service(24);
        service.issueAndSend(user);
        service.issueAndSend(user);
        String first = ((VerificationEmailRequested) events.get(0)).rawToken();
        String second = ((VerificationEmailRequested) events.get(1)).rawToken();
        assertFalse(service.verify(first));
        assertTrue(service.verify(second));
    }

    @Test
    void welcomeEmailMasksAccountAndQrHoldsOnlyPaymentId() throws Exception {
        List<EmailMessage> sent = new ArrayList<>();
        EmailService email = new EmailService(capturing(sent), "http://localhost:5173");
        String account = "FF1234567890940A";
        assertTrue(email.sendWelcome("asha@example.test", "Asha Rao", account, "asha@finflow"));

        EmailMessage message = sent.getFirst();
        assertFalse(message.text().contains(account));
        assertFalse(message.html().contains(account));
        assertTrue(message.text().contains("•••• 940A"));

        var image = ImageIO.read(new ByteArrayInputStream(message.images().getFirst().bytes()));
        String decoded = new QRCodeReader().decode(new BinaryBitmap(new HybridBinarizer(new BufferedImageLuminanceSource(image)))).getText();
        assertEquals("finflow:pay:v1:asha@finflow", decoded);
    }

    @Test
    void welcomeEmailOmitsQrWithoutPaymentIdentity() {
        List<EmailMessage> sent = new ArrayList<>();
        new EmailService(capturing(sent), "http://localhost:5173").sendWelcome("a@example.test", "A", "FF0000000000ABCD", null);
        assertTrue(sent.getFirst().images().isEmpty());
        assertFalse(sent.getFirst().html().contains("cid:"));
    }

    @Test
    void verificationLinkUsesBaseUrlAndFailedDeliveryDoesNotThrow() {
        List<EmailMessage> sent = new ArrayList<>();
        assertTrue(new EmailService(capturing(sent), "https://app.example/").sendVerification("a@example.test", "A", "tok123", 24));
        assertTrue(sent.getFirst().text().contains("https://app.example/verify-email?token=tok123"));

        EmailProvider failing = new EmailProvider() {
            public String name() { return "SMTP"; }
            public boolean deliversMail() { return true; }
            public void send(EmailMessage message) throws Exception { throw new Exception("connection refused"); }
        };
        assertFalse(new EmailService(failing, "http://localhost:5173").sendVerification("a@example.test", "A", "tok", 24));
        assertEquals("a***@example.test", EmailService.maskEmail("asha@example.test"));
    }

    private static EmailProvider capturing(List<EmailMessage> sent) {
        return new EmailProvider() {
            public String name() { return "TEST"; }
            public boolean deliversMail() { return true; }
            public void send(EmailMessage message) { sent.add(message); }
        };
    }

    private static User user() {
        return new User("Asha Rao", "asha@example.test", "9876543210", "hash", LocalDateTime.now());
    }

    private EmailVerificationService service(long ttlHours) {
        EmailVerificationTokenRepository repository = (EmailVerificationTokenRepository) Proxy.newProxyInstance(
                EmailVerificationTokenRepository.class.getClassLoader(), new Class[]{EmailVerificationTokenRepository.class},
                (proxy, method, args) -> switch (method.getName()) {
                    case "save" -> { tokens.add((EmailVerificationToken) args[0]); yield args[0]; }
                    case "findByTokenHash" -> tokens.stream().filter(t -> t.getTokenHash().equals(args[0])).findFirst();
                    case "findFirstByUserIdOrderByCreatedAtDesc" -> tokens.stream()
                            .filter(t -> Objects.equals(t.getUser().getId(), args[0]))
                            .max(Comparator.comparing(EmailVerificationToken::getCreatedAt));
                    case "countByUserIdAndCreatedAtAfter" -> tokens.stream()
                            .filter(t -> Objects.equals(t.getUser().getId(), args[0]) && t.getCreatedAt().isAfter((LocalDateTime) args[1])).count();
                    case "invalidateOpenTokens" -> {
                        int n = 0;
                        for (EmailVerificationToken t : tokens) {
                            if (Objects.equals(t.getUser().getId(), args[0]) && t.getUsedAt() == null) { t.setUsedAt((LocalDateTime) args[1]); n++; }
                        }
                        yield n;
                    }
                    default -> method.getReturnType() == Optional.class ? Optional.empty() : null;
                });
        UserRepository users = (UserRepository) Proxy.newProxyInstance(
                UserRepository.class.getClassLoader(), new Class[]{UserRepository.class},
                (proxy, method, args) -> "save".equals(method.getName()) ? args[0] : null);
        return new EmailVerificationService(repository, users, events::add, ttlHours);
    }
}
