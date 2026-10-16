package com.finflow.payment;

import com.finflow.dto.AccountResponse;
import com.finflow.dto.TransferRequest;
import com.finflow.service.AccountService;
import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

@Component
@Primary
public class FinFlowInternalPaymentProvider implements PaymentProvider {
    private final AccountService accountService;

    public FinFlowInternalPaymentProvider(AccountService accountService) {
        this.accountService = accountService;
    }

    @Override
    public String providerName() {
        return "FINFLOW_INTERNAL";
    }

    @Override
    public boolean supportsMode(String mode) {
        return true; // Supports all demo/internal modes
    }

    @Override
    public AccountResponse executeInternal(PaymentCommand command) {
        TransferRequest request = new TransferRequest();
        request.setReceiverAccountNumber(command.receiverAccountNumber());
        request.setAmount(command.amount());
        request.setCategory(command.category());
        request.setDescription(command.description());
        return accountService.transferAfterPinVerification(command.senderAccountId(), request, command.idempotencyKey());
    }

    @Override
    public PaymentResult createPayment(String senderHandle, String receiverHandle, BigDecimal amount, String category, String description, String idempotencyKey) {
        String ref = "FIN-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase();
        return new PaymentResult(ref, PaymentStatus.SUCCESS, amount, ref, "Internal payment completed", LocalDateTime.now());
    }

    @Override
    public PaymentResult getPaymentStatus(String paymentId) {
        return new PaymentResult(paymentId, PaymentStatus.SUCCESS, BigDecimal.ZERO, paymentId, "Internal payment verified", LocalDateTime.now());
    }

    @Override
    public PaymentResult refundPayment(String paymentId, BigDecimal amount, String reason) {
        String ref = "REFUND-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase();
        return new PaymentResult(ref, PaymentStatus.REFUNDED, amount, ref, "Refund executed: " + reason, LocalDateTime.now());
    }

    @Override
    public MandateResult createMandate(String userHandle, String merchantName, BigDecimal amount, BigDecimal maxAmount, String frequency, String idempotencyKey) {
        String mandateId = "MND-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase();
        return new MandateResult(mandateId, "PRV-MND-" + mandateId, PaymentStatus.ACTIVE, "Mandate created and active", LocalDateTime.now());
    }

    @Override
    public MandateResult executeMandate(String providerMandateId, BigDecimal amount, String idempotencyKey) {
        return new MandateResult(providerMandateId, providerMandateId, PaymentStatus.SUCCESS, "Mandate executed", LocalDateTime.now());
    }

    @Override
    public MandateResult pauseMandate(String providerMandateId) {
        return new MandateResult(providerMandateId, providerMandateId, PaymentStatus.PAUSED, "Mandate paused", LocalDateTime.now());
    }

    @Override
    public MandateResult resumeMandate(String providerMandateId) {
        return new MandateResult(providerMandateId, providerMandateId, PaymentStatus.ACTIVE, "Mandate resumed", LocalDateTime.now());
    }

    @Override
    public MandateResult revokeMandate(String providerMandateId) {
        return new MandateResult(providerMandateId, providerMandateId, PaymentStatus.CANCELLED, "Mandate revoked", LocalDateTime.now());
    }
}
