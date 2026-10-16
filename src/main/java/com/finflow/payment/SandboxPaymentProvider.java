package com.finflow.payment;

import com.finflow.dto.AccountResponse;
import com.finflow.dto.TransferRequest;
import com.finflow.service.AccountService;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

@Component
public class SandboxPaymentProvider implements PaymentProvider {

    private final AccountService accountService;

    public SandboxPaymentProvider(AccountService accountService) {
        this.accountService = accountService;
    }

    @Override
    public String providerName() {
        return "FINFLOW_SANDBOX_PROVIDER";
    }

    @Override
    public boolean supportsMode(String mode) {
        return "SANDBOX".equalsIgnoreCase(mode) || "DEMO".equalsIgnoreCase(mode);
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
        String ref = "SANDBOX-PAY-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase();
        return new PaymentResult(ref, PaymentStatus.SUCCESS, amount, ref, "Sandbox simulated payment completed successfully", LocalDateTime.now());
    }

    @Override
    public PaymentResult getPaymentStatus(String paymentId) {
        return new PaymentResult(paymentId, PaymentStatus.SUCCESS, BigDecimal.ZERO, paymentId, "Sandbox payment verified", LocalDateTime.now());
    }

    @Override
    public PaymentResult refundPayment(String paymentId, BigDecimal amount, String reason) {
        String ref = "SANDBOX-REFUND-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase();
        return new PaymentResult(ref, PaymentStatus.REFUNDED, amount, ref, "Sandbox refund executed: " + reason, LocalDateTime.now());
    }

    @Override
    public MandateResult createMandate(String userHandle, String merchantName, BigDecimal amount, BigDecimal maxAmount, String frequency, String idempotencyKey) {
        String mandateId = "SANDBOX-MND-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase();
        return new MandateResult(mandateId, "SANDBOX-PRV-MND-" + mandateId, PaymentStatus.ACTIVE, "Sandbox mandate created and activated", LocalDateTime.now());
    }

    @Override
    public MandateResult executeMandate(String providerMandateId, BigDecimal amount, String idempotencyKey) {
        return new MandateResult(providerMandateId, providerMandateId, PaymentStatus.SUCCESS, "Sandbox mandate executed successfully", LocalDateTime.now());
    }

    @Override
    public MandateResult pauseMandate(String providerMandateId) {
        return new MandateResult(providerMandateId, providerMandateId, PaymentStatus.PAUSED, "Sandbox mandate paused", LocalDateTime.now());
    }

    @Override
    public MandateResult resumeMandate(String providerMandateId) {
        return new MandateResult(providerMandateId, providerMandateId, PaymentStatus.ACTIVE, "Sandbox mandate resumed", LocalDateTime.now());
    }

    @Override
    public MandateResult revokeMandate(String providerMandateId) {
        return new MandateResult(providerMandateId, providerMandateId, PaymentStatus.CANCELLED, "Sandbox mandate revoked", LocalDateTime.now());
    }
}
