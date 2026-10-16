package com.finflow.autopay;

import java.math.BigDecimal;
import java.time.LocalDate;

public interface AutopayProvider {
    Authorization createAuthorization(
            String merchantName,
            String provider,
            Long accountId,
            BigDecimal amount,
            BigDecimal maximumAmount,
            String frequency,
            LocalDate nextPaymentDate);

    Authorization pauseAuthorization(String providerAuthorizationReference);

    Authorization resumeAuthorization(String providerAuthorizationReference);

    Authorization cancelAuthorization(String providerAuthorizationReference);

    Authorization executePayment(String providerAuthorizationReference, BigDecimal amount);

    Authorization getAuthorizationStatus(String providerAuthorizationReference);

    record Authorization(String tokenReference, String providerReference, String status) {
    }
}
