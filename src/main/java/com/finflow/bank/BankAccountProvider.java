package com.finflow.bank;

public interface BankAccountProvider {
    String providerName();

    LinkResult initiateLink(BankLinkCommand command);

    VerificationResult verifyLink(String providerLinkReference, String sandboxConfirmation);

    VerificationResult getAccountStatus(String providerLinkReference);

    VerificationResult unlinkAccount(String providerLinkReference);

    record BankLinkCommand(
            String bankName,
            String bankCode,
            String accountHolderName,
            String accountNumber,
            String accountType) {
    }

    record LinkResult(
            String providerLinkReference,
            String providerAccountReference,
            String status,
            String verificationHint,
            String message) {
    }

    record VerificationResult(
            String providerLinkReference,
            String providerAccountReference,
            String status,
            String message) {
    }
}
