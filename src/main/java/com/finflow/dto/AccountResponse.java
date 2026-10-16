package com.finflow.dto;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public class AccountResponse {

    private Long id;
    private String accountNumber;
    private BigDecimal balance;
    private String accountType;
    private String status;
    private boolean isPrimary;
    private LocalDateTime createdAt;

    public AccountResponse(
            Long id,
            String accountNumber,
            BigDecimal balance,
            String accountType,
            String status,
            LocalDateTime createdAt) {
        this(id, accountNumber, balance, accountType, status, false, createdAt);
    }

    public AccountResponse(
            Long id,
            String accountNumber,
            BigDecimal balance,
            String accountType,
            String status,
            boolean isPrimary,
            LocalDateTime createdAt) {

        this.id = id;
        this.accountNumber = accountNumber;
        this.balance = balance;
        this.accountType = accountType;
        this.status = status;
        this.isPrimary = isPrimary;
        this.createdAt = createdAt;
    }

    public Long getId() {
        return id;
    }

    public String getAccountNumber() {
        return accountNumber;
    }

    public BigDecimal getBalance() {
        return balance;
    }

    public String getAccountType() {
        return accountType;
    }

    public String getStatus() {
        return status;
    }

    public boolean isPrimary() {
        return isPrimary;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }
}