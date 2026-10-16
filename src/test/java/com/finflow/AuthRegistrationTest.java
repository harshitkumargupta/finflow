package com.finflow;

import com.finflow.dto.RegisterRequest;
import com.finflow.entity.User;
import com.finflow.exception.ResourceAlreadyExistsException;
import com.finflow.repository.UserRepository;
import com.finflow.service.AuthService;
import com.finflow.service.EmailVerificationService;
import com.finflow.service.SecurityAuditService;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import org.junit.jupiter.api.Test;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import tools.jackson.databind.DeserializationFeature;
import tools.jackson.databind.json.JsonMapper;

import java.lang.reflect.Proxy;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.*;

class AuthRegistrationTest {

    private final BCryptPasswordEncoder encoder = new BCryptPasswordEncoder();

    @Test
    void registerIgnoresPrivilegedFieldsInPayload() throws Exception {
        String json = """
                {"name":" Mallory ","email":" mallory@example.test ","phone":"9876543210",
                 "password":"correct-horse","role":"ADMIN","transactionPinHash":"$2a$10$attacker",
                 "failedPinAttempts":-5,"id":1}
                """;
        RegisterRequest request = JsonMapper.builder()
                .disable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES)
                .build()
                .readValue(json, RegisterRequest.class);

        AtomicReference<User> saved = new AtomicReference<>();
        authService(saved, false, false).register(request);

        User user = saved.get();
        assertNotNull(user);
        assertEquals("USER", user.getRole());
        assertNull(user.getTransactionPinHash());
        assertEquals(0, user.getFailedPinAttempts());
        assertNull(user.getPinLockedUntil());
        assertNull(user.getId());
        assertEquals("Mallory", user.getName());
        assertEquals("mallory@example.test", user.getEmail());
        assertNotNull(user.getCreatedAt());
        assertNotEquals("correct-horse", user.getPassword());
        assertTrue(encoder.matches("correct-horse", user.getPassword()));
    }

    @Test
    void duplicateEmailOrPhoneIsRejected() {
        assertThrows(ResourceAlreadyExistsException.class,
                () -> authService(new AtomicReference<>(), true, false).register(validRequest()));
        assertThrows(ResourceAlreadyExistsException.class,
                () -> authService(new AtomicReference<>(), false, true).register(validRequest()));
    }

    @Test
    void validationRejectsBadInput() {
        Validator validator = Validation.buildDefaultValidatorFactory().getValidator();
        assertTrue(validator.validate(validRequest()).isEmpty());

        RegisterRequest badPhone = validRequest();
        badPhone.setPhone("12345");
        assertFalse(validator.validate(badPhone).isEmpty());

        RegisterRequest shortPassword = validRequest();
        shortPassword.setPassword("short");
        assertFalse(validator.validate(shortPassword).isEmpty());

        RegisterRequest badEmail = validRequest();
        badEmail.setEmail("not-an-email");
        assertFalse(validator.validate(badEmail).isEmpty());
    }

    private RegisterRequest validRequest() {
        RegisterRequest request = new RegisterRequest();
        request.setName("Test User");
        request.setEmail("user@example.test");
        request.setPhone("9876543210");
        request.setPassword("password123");
        return request;
    }

    private AuthService authService(AtomicReference<User> saved, boolean emailExists, boolean phoneExists) {
        UserRepository repository = (UserRepository) Proxy.newProxyInstance(
                UserRepository.class.getClassLoader(), new Class[]{UserRepository.class},
                (proxy, method, args) -> switch (method.getName()) {
                    case "existsByEmail" -> emailExists;
                    case "existsByPhone" -> phoneExists;
                    case "save" -> { saved.set((User) args[0]); yield args[0]; }
                    default -> method.getReturnType() == boolean.class ? false : null;
                });
        return new AuthService(repository, encoder, null, null, new NoopAudit(), new NoopVerification());
    }

    private static final class NoopVerification extends EmailVerificationService {
        private NoopVerification() { super(null, null, null, 24); }
        @Override public void issueAndSend(User user) { }
    }

    private static final class NoopAudit extends SecurityAuditService {
        private NoopAudit() { super(null, null); }
        @Override public void log(User user, String action, String resource, String referenceId, String riskLevel, String status, String detail) { }
    }
}
