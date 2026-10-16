package com.finflow.service;

import com.finflow.entity.User;
import com.finflow.entity.Wallet;
import com.finflow.repository.WalletRepository;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;

@Service
public class PaymentPolicyEngine {

    public enum DecisionResult {
        ALLOW, DENY, REQUIRE_APPROVAL
    }

    public record PolicyDecision(DecisionResult result, String reason, String code) {}

    private final WalletRepository walletRepository;

    public PaymentPolicyEngine(WalletRepository walletRepository) {
        this.walletRepository = walletRepository;
    }

    public PolicyDecision evaluatePayment(User sender, BigDecimal amount, String category, boolean checkWalletBalance) {
        if (amount == null || amount.compareTo(BigDecimal.ZERO) <= 0) {
            return new PolicyDecision(DecisionResult.DENY, "Payment amount must be greater than zero", "INVALID_AMOUNT");
        }

        // Rule 1: Wallet status check
        Wallet wallet = walletRepository.findByUserId(sender.getId()).orElse(null);
        if (wallet == null || !"ACTIVE".equalsIgnoreCase(wallet.getStatus())) {
            return new PolicyDecision(DecisionResult.DENY, "Sender FinFlow wallet is inactive or frozen", "WALLET_INACTIVE");
        }

        // Rule 2: Single payment upper limit
        if (amount.compareTo(BigDecimal.valueOf(100000)) > 0) {
            return new PolicyDecision(DecisionResult.DENY, "Single payment exceeds safety limit of ₹1,00,000", "EXCEEDS_SINGLE_LIMIT");
        }

        // Rule 3: Balance check
        if (checkWalletBalance && wallet.getBalance().compareTo(amount) < 0) {
            return new PolicyDecision(DecisionResult.DENY, "Insufficient spendable wallet balance", "INSUFFICIENT_FUNDS");
        }

        // Rule 4: High amount recommendation check
        if (amount.compareTo(BigDecimal.valueOf(25000)) > 0) {
            return new PolicyDecision(DecisionResult.REQUIRE_APPROVAL, "High value transaction requires double confirmation", "HIGH_VALUE_CONFIRMATION");
        }

        return new PolicyDecision(DecisionResult.ALLOW, "Payment authorized by policy engine", "AUTHORIZED");
    }
}
