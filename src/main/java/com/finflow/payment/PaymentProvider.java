package com.finflow.payment;

import com.finflow.dto.AccountResponse;
import java.math.BigDecimal;

public interface PaymentProvider {
    String providerName();
    boolean supportsMode(String mode);
    
    AccountResponse executeInternal(PaymentCommand command);

    PaymentResult createPayment(String senderHandle, String receiverHandle, BigDecimal amount, String category, String description, String idempotencyKey);
    PaymentResult getPaymentStatus(String paymentId);
    PaymentResult refundPayment(String paymentId, BigDecimal amount, String reason);

    MandateResult createMandate(String userHandle, String merchantName, BigDecimal amount, BigDecimal maxAmount, String frequency, String idempotencyKey);
    MandateResult executeMandate(String providerMandateId, BigDecimal amount, String idempotencyKey);
    MandateResult pauseMandate(String providerMandateId);
    MandateResult resumeMandate(String providerMandateId);
    MandateResult revokeMandate(String providerMandateId);
}
