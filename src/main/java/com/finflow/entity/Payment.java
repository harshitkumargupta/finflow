package com.finflow.entity;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(
        name = "payments",
        indexes = {
                @Index(name = "idx_payment_user_created", columnList = "user_id, created_at"),
                @Index(name = "idx_payment_idempotency_key", columnList = "idempotency_key")
        }
)
public class Payment {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "reference_number", nullable = false, unique = true, length = 40)
    private String referenceNumber;

    @Column(name = "external_provider_reference", length = 120)
    private String externalProviderReference;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Column(name = "source_type", nullable = false, length = 30)
    private String sourceType; // FINFLOW_ACCOUNT, FINFLOW_WALLET, CARD, UPI

    @Column(name = "source_id")
    private Long sourceId;

    @Column(name = "recipient_type", nullable = false, length = 30)
    private String recipientType; // FINFLOW_ID, BANK_ACCOUNT, UPI_ID, QR_CODE

    @Column(name = "recipient_identifier", nullable = false, length = 120)
    private String recipientIdentifier;

    @Column(name = "recipient_name", length = 120)
    private String recipientName;

    @Column(nullable = false, precision = 19, scale = 2)
    private BigDecimal amount;

    @Column(nullable = false, length = 3)
    private String currency = "INR";

    @Column(nullable = false, length = 30)
    private String status; // CREATED, PENDING, AUTHORIZED, PROCESSING, SUCCESS, FAILED, CANCELLED, REFUNDED

    @Column(length = 50)
    private String category;

    @Column(length = 255)
    private String description;

    @Column(name = "failure_reason", length = 255)
    private String failureReason;

    @Column(name = "idempotency_key", unique = true, length = 100)
    private String idempotencyKey;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    @Column(name = "completed_at")
    private LocalDateTime completedAt;

    public Payment() {
    }

    public Long getId() {
        return id;
    }

    public String getReferenceNumber() {
        return referenceNumber;
    }

    public void setReferenceNumber(String referenceNumber) {
        this.referenceNumber = referenceNumber;
    }

    public String getExternalProviderReference() {
        return externalProviderReference;
    }

    public void setExternalProviderReference(String externalProviderReference) {
        this.externalProviderReference = externalProviderReference;
    }

    public User getUser() {
        return user;
    }

    public void setUser(User user) {
        this.user = user;
    }

    public String getSourceType() {
        return sourceType;
    }

    public void setSourceType(String sourceType) {
        this.sourceType = sourceType;
    }

    public Long getSourceId() {
        return sourceId;
    }

    public void setSourceId(Long sourceId) {
        this.sourceId = sourceId;
    }

    public String getRecipientType() {
        return recipientType;
    }

    public void setRecipientType(String recipientType) {
        this.recipientType = recipientType;
    }

    public String getRecipientIdentifier() {
        return recipientIdentifier;
    }

    public void setRecipientIdentifier(String recipientIdentifier) {
        this.recipientIdentifier = recipientIdentifier;
    }

    public String getRecipientName() {
        return recipientName;
    }

    public void setRecipientName(String recipientName) {
        this.recipientName = recipientName;
    }

    public BigDecimal getAmount() {
        return amount;
    }

    public void setAmount(BigDecimal amount) {
        this.amount = amount;
    }

    public String getCurrency() {
        return currency;
    }

    public void setCurrency(String currency) {
        this.currency = currency;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public String getCategory() {
        return category;
    }

    public void setCategory(String category) {
        this.category = category;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getFailureReason() {
        return failureReason;
    }

    public void setFailureReason(String failureReason) {
        this.failureReason = failureReason;
    }

    public String getIdempotencyKey() {
        return idempotencyKey;
    }

    public void setIdempotencyKey(String idempotencyKey) {
        this.idempotencyKey = idempotencyKey;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public LocalDateTime getCompletedAt() {
        return completedAt;
    }

    public void setCompletedAt(LocalDateTime completedAt) {
        this.completedAt = completedAt;
    }
}
