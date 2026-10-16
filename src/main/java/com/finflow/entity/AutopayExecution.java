package com.finflow.entity;

import jakarta.persistence.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "autopay_executions",
        uniqueConstraints = {
                @UniqueConstraint(name = "uk_autopay_execution_idempotency",
                        columnNames = "idempotency_key"),
                @UniqueConstraint(name = "uk_autopay_execution_authorization_idempotency",
                        columnNames = {"autopay_authorization_id", "idempotency_key"})},
        indexes = {
                @Index(name = "idx_autopay_exec_user_date", columnList = "user_id,executed_at"),
                @Index(name = "idx_autopay_exec_auth_date", columnList = "autopay_authorization_id,executed_at")
        })
public class AutopayExecution {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "autopay_authorization_id", nullable = false)
    private AutopayAuthorization autopayAuthorization;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Column(nullable = false, precision = 19, scale = 2)
    private BigDecimal amount;

    @Column(nullable = false, length = 30)
    private String status; // SUCCESS, FAILED

    @Column(length = 255)
    private String failureReason;

    @Column(name = "transaction_reference", length = 100)
    private String transactionReference;

    @Column(name = "transaction_id")
    private Long transactionId;

    @Column(name = "provider_reference", length = 160)
    private String providerReference;

    @Column(name = "idempotency_key", length = 120)
    private String idempotencyKey;

    @Column(name = "is_simulated", nullable = false)
    private boolean isSimulated = true;

    @Column(name = "executed_at", nullable = false)
    private LocalDateTime executedAt;

    public AutopayExecution() {
    }

    public Long getId() {
        return id;
    }

    public AutopayAuthorization getAutopayAuthorization() {
        return autopayAuthorization;
    }

    public void setAutopayAuthorization(AutopayAuthorization autopayAuthorization) {
        this.autopayAuthorization = autopayAuthorization;
    }

    public User getUser() {
        return user;
    }

    public void setUser(User user) {
        this.user = user;
    }

    public BigDecimal getAmount() {
        return amount;
    }

    public void setAmount(BigDecimal amount) {
        this.amount = amount;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public String getFailureReason() {
        return failureReason;
    }

    public void setFailureReason(String failureReason) {
        this.failureReason = failureReason;
    }

    public String getTransactionReference() {
        return transactionReference;
    }

    public void setTransactionReference(String transactionReference) {
        this.transactionReference = transactionReference;
    }

    public boolean isSimulated() {
        return isSimulated;
    }

    public void setSimulated(boolean simulated) {
        isSimulated = simulated;
    }

    public LocalDateTime getExecutedAt() {
        return executedAt;
    }

    public void setExecutedAt(LocalDateTime executedAt) {
        this.executedAt = executedAt;
    }

    public Long getTransactionId() { return transactionId; }
    public void setTransactionId(Long transactionId) { this.transactionId = transactionId; }
    public String getProviderReference() { return providerReference; }
    public void setProviderReference(String providerReference) { this.providerReference = providerReference; }
    public String getIdempotencyKey() { return idempotencyKey; }
    public void setIdempotencyKey(String idempotencyKey) { this.idempotencyKey = idempotencyKey; }
}
