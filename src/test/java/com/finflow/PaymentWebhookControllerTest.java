package com.finflow;

import com.finflow.controller.PaymentWebhookController;
import com.finflow.dto.PaymentWebhookRequest;
import com.finflow.entity.PaymentWebhookEvent;
import com.finflow.entity.User;
import com.finflow.repository.PaymentWebhookEventRepository;
import com.finflow.service.SecurityAuditService;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.lang.reflect.Proxy;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.HashSet;
import java.util.HexFormat;
import java.util.Map;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

class PaymentWebhookControllerTest {

    private static final String SECRET = "test-webhook-secret";
    private static final String PROVIDER = "FINFLOW_SANDBOX_PROVIDER";

    private final Set<String> stored = new HashSet<>();
    private final PaymentWebhookController controller = new PaymentWebhookController(
            new NoopAudit(), repository(), SECRET, 300, PROVIDER);

    @Test
    void validSignatureIsAccepted() {
        PaymentWebhookRequest request = request("evt-1", Instant.now().getEpochSecond());
        ResponseEntity<Map<String, Object>> response = controller.handleWebhook(PROVIDER, sign(PROVIDER, request), request);
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertEquals("RECEIVED", response.getBody().get("status"));
        assertTrue(stored.contains(PROVIDER + "|evt-1"));
    }

    @Test
    void badSignatureIsRejectedWithoutDetail() {
        PaymentWebhookRequest request = request("evt-2", Instant.now().getEpochSecond());
        ResponseEntity<Map<String, Object>> response = controller.handleWebhook(PROVIDER, "sha256=" + "00".repeat(32), request);
        assertEquals(HttpStatus.UNAUTHORIZED, response.getStatusCode());
        assertNull(response.getBody());
        assertTrue(stored.isEmpty());

        assertEquals(HttpStatus.UNAUTHORIZED, controller.handleWebhook(PROVIDER, null, request).getStatusCode());
        assertEquals(HttpStatus.UNAUTHORIZED, controller.handleWebhook(PROVIDER, "not-hex", request).getStatusCode());
    }

    @Test
    void unknownProviderIsRejectedWithoutDetail() {
        PaymentWebhookRequest request = request("evt-3", Instant.now().getEpochSecond());
        ResponseEntity<Map<String, Object>> response = controller.handleWebhook("EVIL", sign("EVIL", request), request);
        assertEquals(HttpStatus.UNAUTHORIZED, response.getStatusCode());
        assertNull(response.getBody());
        assertTrue(stored.isEmpty());
    }

    @Test
    void staleTimestampIsRejected() {
        PaymentWebhookRequest request = request("evt-4", Instant.now().getEpochSecond() - 301);
        ResponseEntity<Map<String, Object>> response = controller.handleWebhook(PROVIDER, sign(PROVIDER, request), request);
        assertEquals(HttpStatus.UNAUTHORIZED, response.getStatusCode());
        assertNull(response.getBody());
        assertTrue(stored.isEmpty());
    }

    @Test
    void replayedEventIsNotProcessedTwice() {
        PaymentWebhookRequest request = request("evt-5", Instant.now().getEpochSecond());
        String signature = sign(PROVIDER, request);
        assertEquals(true, controller.handleWebhook(PROVIDER, signature, request).getBody().get("processed"));
        ResponseEntity<Map<String, Object>> replay = controller.handleWebhook(PROVIDER, signature, request);
        assertEquals(HttpStatus.OK, replay.getStatusCode());
        assertEquals("DUPLICATE", replay.getBody().get("status"));
        assertEquals(false, replay.getBody().get("processed"));
        assertEquals(1, stored.size());
    }

    @Test
    void missingSecretDisablesWebhook() {
        PaymentWebhookController unconfigured = new PaymentWebhookController(new NoopAudit(), repository(), "", 300, PROVIDER);
        PaymentWebhookRequest request = request("evt-6", Instant.now().getEpochSecond());
        assertEquals(HttpStatus.SERVICE_UNAVAILABLE, unconfigured.handleWebhook(PROVIDER, sign(PROVIDER, request), request).getStatusCode());
    }

    private PaymentWebhookRequest request(String eventId, long timestamp) {
        return new PaymentWebhookRequest(eventId, "payment.succeeded", "pay-1", "SUCCESS", new BigDecimal("125.50"), timestamp);
    }

    private String sign(String provider, PaymentWebhookRequest request) {
        String canonical = String.join("|", provider, request.eventId(), request.eventType(), request.paymentId(),
                request.status(), request.amount().toPlainString(), String.valueOf(request.timestamp()));
        try {
            Mac mac = Mac.getInstance("HmacSHA256");
            mac.init(new SecretKeySpec(SECRET.getBytes(StandardCharsets.UTF_8), "HmacSHA256"));
            return "sha256=" + HexFormat.of().formatHex(mac.doFinal(canonical.getBytes(StandardCharsets.UTF_8)));
        } catch (Exception exception) {
            throw new AssertionError(exception);
        }
    }

    private PaymentWebhookEventRepository repository() {
        return (PaymentWebhookEventRepository) Proxy.newProxyInstance(
                PaymentWebhookEventRepository.class.getClassLoader(), new Class[]{PaymentWebhookEventRepository.class},
                (proxy, method, args) -> switch (method.getName()) {
                    case "existsByProviderAndEventId" -> stored.contains(args[0] + "|" + args[1]);
                    case "save" -> {
                        PaymentWebhookEvent event = (PaymentWebhookEvent) args[0];
                        stored.add(event.getProvider() + "|" + event.getEventId());
                        yield event;
                    }
                    default -> method.getReturnType() == boolean.class ? false : null;
                });
    }

    private static final class NoopAudit extends SecurityAuditService {
        private NoopAudit() { super(null, null); }
        @Override public void log(User user, String action, String resource, String referenceId, String riskLevel, String status, String detail) { }
    }
}
