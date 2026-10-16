package com.finflow;

import com.finflow.entity.User;
import com.finflow.exception.InvalidTransferException;
import com.finflow.repository.UserRepository;
import com.finflow.service.PinVerificationService;
import com.finflow.service.SecurityAuditService;
import org.junit.jupiter.api.Test;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;

import java.lang.reflect.Proxy;
import java.time.LocalDateTime;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

class PinVerificationServiceTest {
    @Test
    void fiveWrongAttemptsLockAndDoNotExposePin() {
        User user = userWithPin("123456");
        RecordingAudit audit = new RecordingAudit();
        PinVerificationService service = new PinVerificationService(repositoryFor(user), new BCryptPasswordEncoder(), audit);
        for (int attempt = 0; attempt < 5; attempt++) {
            assertThrows(InvalidTransferException.class, () -> service.verify(user, "000000"));
        }
        assertNotNull(user.getPinLockedUntil());
        assertThrows(InvalidTransferException.class, () -> service.verify(user, "123456"));
        assertTrue(audit.details.stream().noneMatch(value -> value.contains("123456") || value.contains("000000")));
    }

    @Test
    void successfulVerificationResetsFailuresAfterLockExpiry() {
        User user = userWithPin("123456");
        user.setFailedPinAttempts(4);
        user.setPinLockedUntil(LocalDateTime.now().minusMinutes(1));
        PinVerificationService service = new PinVerificationService(repositoryFor(user), new BCryptPasswordEncoder(), new RecordingAudit());
        assertSame(user, service.verify(user, "123456"));
        assertEquals(0, user.getFailedPinAttempts());
        assertNull(user.getPinLockedUntil());
    }

    private UserRepository repositoryFor(User user) {
        return (UserRepository) Proxy.newProxyInstance(UserRepository.class.getClassLoader(), new Class[]{UserRepository.class},
                (proxy, method, args) -> method.getName().equals("findByIdForUpdate") ? Optional.of(user) : defaultValue(method.getReturnType()));
    }

    private Object defaultValue(Class<?> type) { return type == boolean.class ? false : type == int.class ? 0 : null; }

    private User userWithPin(String pin) {
        User user = new User("Test", "pin@example.test", "9876543210", "hashed-password", LocalDateTime.now());
        try { var field = User.class.getDeclaredField("id"); field.setAccessible(true); field.set(user, 1L); }
        catch (ReflectiveOperationException exception) { throw new AssertionError(exception); }
        user.setTransactionPinHash(new BCryptPasswordEncoder().encode(pin));
        return user;
    }

    private static final class RecordingAudit extends SecurityAuditService {
        private final java.util.List<String> details = new java.util.ArrayList<>();
        private RecordingAudit() { super(null, null); }
        @Override public void log(User user, String action, String resource, String referenceId, String riskLevel, String status, String detail) { details.add(detail); }
    }
}
