package com.finflow.service;

import com.finflow.autopay.AutopayProvider;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

@Service
public class InternalAutopayProvider implements AutopayProvider {

    private final ConcurrentHashMap<String, String> authorizationStatuses = new ConcurrentHashMap<>();

    @Override
    public Authorization createAuthorization(
            String merchantName,
            String provider,
            Long accountId,
            BigDecimal amount,
            BigDecimal maximumAmount,
            String frequency,
            LocalDate nextPaymentDate) {
        
        String tokenReference = "TOK-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase();
        String providerReference = "AUTH-" + UUID.randomUUID().toString().substring(0, 12).toUpperCase();
        
        authorizationStatuses.put(providerReference, "ACTIVE");
        return new Authorization(tokenReference, providerReference, "ACTIVE");
    }

    @Override
    public Authorization pauseAuthorization(String providerAuthorizationReference) {
        if (authorizationStatuses.containsKey(providerAuthorizationReference)) {
            authorizationStatuses.put(providerAuthorizationReference, "PAUSED");
            return new Authorization("N/A", providerAuthorizationReference, "PAUSED");
        }
        return new Authorization("N/A", providerAuthorizationReference, "UNKNOWN");
    }

    @Override
    public Authorization resumeAuthorization(String providerAuthorizationReference) {
        if (authorizationStatuses.containsKey(providerAuthorizationReference)) {
            authorizationStatuses.put(providerAuthorizationReference, "ACTIVE");
            return new Authorization("N/A", providerAuthorizationReference, "ACTIVE");
        }
        return new Authorization("N/A", providerAuthorizationReference, "UNKNOWN");
    }

    @Override
    public Authorization cancelAuthorization(String providerAuthorizationReference) {
        if (authorizationStatuses.containsKey(providerAuthorizationReference)) {
            authorizationStatuses.put(providerAuthorizationReference, "CANCELLED");
            return new Authorization("N/A", providerAuthorizationReference, "CANCELLED");
        }
        return new Authorization("N/A", providerAuthorizationReference, "UNKNOWN");
    }

    @Override
    public Authorization executePayment(String providerAuthorizationReference, BigDecimal amount) {
        // In sandbox mode this confirms provider-side execution without moving real money.
        String status = authorizationStatuses.getOrDefault(providerAuthorizationReference, "UNKNOWN");
        if ("ACTIVE".equalsIgnoreCase(status)) {
            return new Authorization("N/A", providerAuthorizationReference, "SUCCESS");
        }
        return new Authorization("N/A", providerAuthorizationReference, status);
    }

    @Override
    public Authorization getAuthorizationStatus(String providerAuthorizationReference) {
        String status = authorizationStatuses.getOrDefault(providerAuthorizationReference, "UNKNOWN");
        return new Authorization("N/A", providerAuthorizationReference, status);
    }
}
