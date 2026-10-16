package com.finflow.bank;

import org.springframework.stereotype.Component;

import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

@Component
public class SandboxBankAccountProvider implements BankAccountProvider {
    private static final String CONFIRMATION_PHRASE = "CONFIRM";

    private final ConcurrentHashMap<String, String> statuses = new ConcurrentHashMap<>();

    @Override
    public String providerName() {
        return "FINFLOW_SANDBOX_BANK_PROVIDER";
    }

    @Override
    public LinkResult initiateLink(BankLinkCommand command) {
        String linkReference = "SBOX-BANK-LINK-" + shortId();
        String accountReference = "SBOX-BANK-ACCT-" + shortId();
        statuses.put(linkReference, "PENDING");
        return new LinkResult(
                linkReference,
                accountReference,
                "PENDING",
                "Sandbox mode: type CONFIRM to verify ownership. No OTP, PIN, or bank password is required.",
                "Sandbox bank-link verification started");
    }

    @Override
    public VerificationResult verifyLink(String providerLinkReference, String sandboxConfirmation) {
        if (!statuses.containsKey(providerLinkReference)) {
            return new VerificationResult(providerLinkReference, null, "FAILED", "Unknown sandbox bank-link reference");
        }
        if (CONFIRMATION_PHRASE.equalsIgnoreCase(sandboxConfirmation == null ? "" : sandboxConfirmation.trim())) {
            statuses.put(providerLinkReference, "VERIFIED");
            return new VerificationResult(providerLinkReference, null, "VERIFIED", "Sandbox ownership verification completed");
        }
        statuses.put(providerLinkReference, "FAILED");
        return new VerificationResult(providerLinkReference, null, "FAILED", "Sandbox confirmation phrase did not match");
    }

    @Override
    public VerificationResult getAccountStatus(String providerLinkReference) {
        String status = statuses.getOrDefault(providerLinkReference, "FAILED");
        return new VerificationResult(providerLinkReference, null, status, "Sandbox bank-link status is " + status);
    }

    @Override
    public VerificationResult unlinkAccount(String providerLinkReference) {
        if (statuses.containsKey(providerLinkReference)) {
            statuses.put(providerLinkReference, "DISCONNECTED");
            return new VerificationResult(providerLinkReference, null, "DISCONNECTED", "Sandbox bank account disconnected");
        }
        return new VerificationResult(providerLinkReference, null, "FAILED", "Unknown sandbox bank-link reference");
    }

    private String shortId() {
        return UUID.randomUUID().toString().replace("-", "").substring(0, 12).toUpperCase();
    }
}
