package com.finflow.dto;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public class TransactionResponse {

    private Long id;
    private String referenceNumber;
    private BigDecimal amount;
    private String type;
    private String category;
    private String categoryConfidence;
    private String categorySource;
    private String categoryReason;
    private String description;
    private String status;
    private LocalDateTime createdAt;
    private String receiverAccountNumber;
    private BigDecimal balanceAfter;

    public TransactionResponse(
            Long id,
            String referenceNumber,
            BigDecimal amount,
            String type,
            String category,
            String categoryConfidence,
            String categorySource,
            String categoryReason,
            String description,
            String status,
            LocalDateTime createdAt,
            String receiverAccountNumber,
            BigDecimal balanceAfter) {

        this.id = id;
        this.referenceNumber = referenceNumber;
        this.amount = amount;
        this.type = type;
        this.category = category;
        this.categoryConfidence = categoryConfidence;
        this.categorySource = categorySource;
        this.categoryReason = categoryReason;
        this.description = description;
        this.status = status;
        this.createdAt = createdAt;
        this.receiverAccountNumber = receiverAccountNumber;
        this.balanceAfter = balanceAfter;
    }

    public Long getId() {
        return id;
    }

    public String getReferenceNumber() {
        return referenceNumber;
    }

    public BigDecimal getAmount() {
        return amount;
    }

    public String getType() {
        return type;
    }

    public String getCategory() {
        return category;
    }

    public String getCategoryConfidence() {
        return categoryConfidence;
    }

    public String getCategorySource() {
        return categorySource;
    }

    public String getCategoryReason() {
        return categoryReason;
    }

    public String getDescription() {
        return description;
    }

    public String getStatus() {
        return status;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public String getReceiverAccountNumber() {
        return receiverAccountNumber;
    }

    public BigDecimal getBalanceAfter() {
        return balanceAfter;
    }
}
