package com.finflow.controller;

import com.finflow.dto.PaymentWebhookRequest;
import com.finflow.entity.PaymentWebhookEvent;
import com.finflow.repository.PaymentWebhookEventRepository;
import com.finflow.service.SecurityAuditService;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.Instant;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.HexFormat;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/payments/webhook")
public class PaymentWebhookController {

    private final SecurityAuditService auditService;
    private final PaymentWebhookEventRepository eventRepository;
    private final String secret;
    private final long toleranceSeconds;
    private final Set<String> providers;

    public PaymentWebhookController(SecurityAuditService auditService, PaymentWebhookEventRepository eventRepository,
                                    @Value("${finflow.webhook.secret:}") String secret,
                                    @Value("${finflow.webhook.timestamp-tolerance-seconds:300}") long toleranceSeconds,
                                    @Value("${finflow.webhook.providers:FINFLOW_SANDBOX_PROVIDER}") String providers) {
        this.auditService = auditService;
        this.eventRepository = eventRepository;
        this.secret = secret;
        this.toleranceSeconds = toleranceSeconds;
        this.providers = Arrays.stream(providers.split(","))
                .map(String::trim)
                .filter(value -> !value.isEmpty())
                .collect(Collectors.toUnmodifiableSet());
    }

    @PostMapping("/{provider}")
    public ResponseEntity<Map<String, Object>> handleWebhook(
            @PathVariable String provider,
            @RequestHeader(value = "X-FinFlow-Signature", required = false) String signature,
            @Valid @RequestBody PaymentWebhookRequest request) {
        if (secret == null || secret.isBlank())
            return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE).build();
        if (!providers.contains(provider))
            return reject(provider, request, "UNKNOWN_PROVIDER");
        if (!validSignature(provider, request, signature))
            return reject(provider, request, "BAD_SIGNATURE");
        if (Math.abs(Instant.now().getEpochSecond() - request.timestamp()) > toleranceSeconds)
            return reject(provider, request, "STALE_TIMESTAMP");
        if (eventRepository.existsByProviderAndEventId(provider, request.eventId()))
            return ResponseEntity.ok(Map.of("status", "DUPLICATE", "provider", provider, "processed", false));
        try {
            eventRepository.save(new PaymentWebhookEvent(provider, request.eventId(), request.eventType(), canonical(provider, request), "RECEIVED", LocalDateTime.now()));
        } catch (DataIntegrityViolationException duplicate) {
            return ResponseEntity.ok(Map.of("status", "DUPLICATE", "provider", provider, "processed", false));
        }
        auditService.log(null, "WEBHOOK_VERIFIED", "Provider/" + provider, request.eventId(), "LOW", "SUCCESS", "Verified webhook event received.");
        return ResponseEntity.ok(Map.of("status", "RECEIVED", "provider", provider, "processed", true, "mode", "SANDBOX"));
    }

    private ResponseEntity<Map<String, Object>> reject(String provider, PaymentWebhookRequest request, String reason) {
        auditService.log(null, "WEBHOOK_REJECTED", "Provider/" + provider, request.eventId(), "HIGH", "FAILED", reason);
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
    }

    private boolean validSignature(String provider, PaymentWebhookRequest request, String signature) {
        if (signature == null || signature.isBlank()) return false;
        try {
            Mac mac = Mac.getInstance("HmacSHA256");
            mac.init(new SecretKeySpec(secret.getBytes(StandardCharsets.UTF_8), "HmacSHA256"));
            byte[] expected = mac.doFinal(canonical(provider, request).getBytes(StandardCharsets.UTF_8));
            String supplied = signature.startsWith("sha256=") ? signature.substring(7) : signature;
            return MessageDigest.isEqual(expected, HexFormat.of().parseHex(supplied));
        } catch (Exception ignored) { return false; }
    }

    private String canonical(String provider, PaymentWebhookRequest request) {
        return String.join("|", provider, request.eventId(), request.eventType(), request.paymentId(), request.status(), request.amount().toPlainString(), String.valueOf(request.timestamp()));
    }
}
