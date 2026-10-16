package com.finflow.service;

import com.finflow.entity.Transaction;
import com.finflow.entity.User;
import com.finflow.repository.TransactionRepository;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.List;

@Service
public class TransactionRiskService {

    public enum RiskLevel {
        LOW, MEDIUM, HIGH, BLOCKED
    }

    public record RiskAssessment(RiskLevel level, String reason, int riskScore) {}

    private final TransactionRepository transactionRepository;

    public TransactionRiskService(TransactionRepository transactionRepository) {
        this.transactionRepository = transactionRepository;
    }

    public RiskAssessment evaluateTransaction(User user, BigDecimal amount, String recipientIdentifier, String category) {
        if (amount == null || amount.compareTo(BigDecimal.ZERO) <= 0) {
            return new RiskAssessment(RiskLevel.BLOCKED, "Invalid transaction amount", 100);
        }

        int score = 0;
        StringBuilder reasons = new StringBuilder();

        // Rule 1: Abnormally large single transaction
        if (amount.compareTo(BigDecimal.valueOf(100000)) > 0) {
            return new RiskAssessment(RiskLevel.BLOCKED, "Transaction amount exceeds maximum permissible single limit of ₹1,00,000", 100);
        } else if (amount.compareTo(BigDecimal.valueOf(50000)) > 0) {
            score += 50;
            reasons.append("High single transaction amount (> ₹50,000). ");
        } else if (amount.compareTo(BigDecimal.valueOf(20000)) > 0) {
            score += 25;
            reasons.append("Moderate single transaction amount (> ₹20,000). ");
        }

        // Rule 2: Deviation from historical average
        List<Transaction> past = transactionRepository.findSuccessfulByUserIdOrderByCreatedAtDesc(user.getId());
        if (!past.isEmpty()) {
            List<BigDecimal> amounts = past.stream().map(Transaction::getAmount).toList();
            BigDecimal sum = amounts.stream().reduce(BigDecimal.ZERO, BigDecimal::add);
            BigDecimal avg = sum.divide(BigDecimal.valueOf(amounts.size()), 2, RoundingMode.HALF_UP);
            
            if (avg.compareTo(BigDecimal.ZERO) > 0 && amount.compareTo(avg.multiply(BigDecimal.valueOf(4))) > 0) {
                score += 35;
                reasons.append("Amount is more than 4x your average transaction size. ");
            }
        }

        // Rule 3: Velocity check (multiple transactions within 2 minutes)
        LocalDateTime twoMinsAgo = LocalDateTime.now().minusMinutes(2);
        long recentCount = past.stream()
                .filter(t -> t.getCreatedAt() != null && t.getCreatedAt().isAfter(twoMinsAgo))
                .count();
        if (recentCount >= 3) {
            score += 40;
            reasons.append("Rapid transaction frequency detected (3+ within 2 minutes). ");
        } else if (recentCount >= 2) {
            score += 20;
            reasons.append("Multiple transactions in a short timeframe. ");
        }

        // Determine final risk level
        RiskLevel level;
        if (score >= 70) {
            level = RiskLevel.HIGH;
        } else if (score >= 35) {
            level = RiskLevel.MEDIUM;
        } else {
            level = RiskLevel.LOW;
        }

        String finalReason = reasons.length() > 0 ? reasons.toString().trim() : "Normal transaction behavior";
        return new RiskAssessment(level, finalReason, score);
    }
}
