package com.finflow.payment;

import com.finflow.dto.AccountResponse;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;

@Component
public class ProductionPaymentProvider implements PaymentProvider {

    @Override
    public String providerName() {
        return "FINFLOW_PRODUCTION_PROVIDER";
    }

    @Override
    public boolean supportsMode(String mode) {
        return "PRODUCTION".equalsIgnoreCase(mode);
    }

    @Override
    public AccountResponse executeInternal(PaymentCommand command) {
        throw new UnsupportedOperationException("Production payment provider requires licensed PSP/PPI partner credentials and live environment configuration.");
    }

    @Override
    public PaymentResult createPayment(String senderHandle, String receiverHandle, BigDecimal amount, String category, String description, String idempotencyKey) {
        throw new UnsupportedOperationException("Production payment provider requires licensed PSP/PPI partner integration.");
    }

    @Override
    public PaymentResult getPaymentStatus(String paymentId) {
        throw new UnsupportedOperationException("Production payment provider requires licensed PSP/PPI partner integration.");
    }

    @Override
    public PaymentResult refundPayment(String paymentId, BigDecimal amount, String reason) {
        throw new UnsupportedOperationException("Production payment provider requires licensed PSP/PPI partner integration.");
    }

    @Override
    public MandateResult createMandate(String userHandle, String merchantName, BigDecimal amount, BigDecimal maxAmount, String frequency, String idempotencyKey) {
        throw new UnsupportedOperationException("Production mandate creation requires licensed PSP/PPI partner integration.");
    }

    @Override
    public MandateResult executeMandate(String providerMandateId, BigDecimal amount, String idempotencyKey) {
        throw new UnsupportedOperationException("Production mandate execution requires licensed PSP/PPI partner integration.");
    }

    @Override
    public MandateResult pauseMandate(String providerMandateId) {
        throw new UnsupportedOperationException("Production mandate control requires licensed PSP/PPI partner integration.");
    }

    @Override
    public MandateResult resumeMandate(String providerMandateId) {
        throw new UnsupportedOperationException("Production mandate control requires licensed PSP/PPI partner integration.");
    }

    @Override
    public MandateResult revokeMandate(String providerMandateId) {
        throw new UnsupportedOperationException("Production mandate control requires licensed PSP/PPI partner integration.");
    }
}
