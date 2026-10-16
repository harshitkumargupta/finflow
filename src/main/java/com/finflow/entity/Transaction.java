package com.finflow.entity;

import jakarta.persistence.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(
        name = "transactions",
        indexes = {
                @Index(
                        name = "idx_transaction_idempotency_key",
                        columnList = "idempotency_key"
                )
        }
)
public class Transaction {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true)
    private String referenceNumber;

    @Column(nullable = false, precision = 19, scale = 2)
    private BigDecimal amount;

    @Column(nullable = false)
    private String type;

    @Column
    private String category;

    @Column(name = "category_confidence")
    private String categoryConfidence;

    @Column(name = "category_source")
    private String categorySource;

    @Column(name = "category_reason")
    private String categoryReason;

    @Column(name = "original_detected_category")
    private String originalDetectedCategory;

    @Column(name = "category_corrected_at")
    private LocalDateTime categoryCorrectedAt;

    @Column
    private String description;

    @Column(nullable = false)
    private String status;

    @Column(nullable = false)
    private LocalDateTime createdAt;

    /*
     * Used only for transfer-out transactions.
     *
     * Nullable because deposits and withdrawals do not
     * require an idempotency key at this stage.
     */
    @Column(
            name = "idempotency_key",
            unique = true,
            length = 100
    )
    private String idempotencyKey;

    /*
     * Stores the receiver account number for a transfer.
     * This allows us to verify that a retry using the same
     * idempotency key represents the same operation.
     */
    @Column(name = "receiver_account_number")
    private String receiverAccountNumber;

    /*
     * Balance after the transaction.
     *
     * This lets us return the original transfer result when
     * the client retries the same idempotency key.
     */
    @Column(
            name = "balance_after",
            precision = 19,
            scale = 2
    )
    private BigDecimal balanceAfter;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "account_id", nullable = false)
    private Account account;

    public Transaction() {
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

    public BigDecimal getAmount() {
        return amount;
    }

    public void setAmount(BigDecimal amount) {
        this.amount = amount;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public String getCategory() {
        return category;
    }

    public void setCategory(String category) {
        this.category = category;
    }

    public String getCategoryConfidence() {
        return categoryConfidence;
    }

    public void setCategoryConfidence(String categoryConfidence) {
        this.categoryConfidence = categoryConfidence;
    }

    public String getCategorySource() {
        return categorySource;
    }

    public void setCategorySource(String categorySource) {
        this.categorySource = categorySource;
    }

    public String getCategoryReason() {
        return categoryReason;
    }

    public void setCategoryReason(String categoryReason) {
        this.categoryReason = categoryReason;
    }

    public String getOriginalDetectedCategory() {
        return originalDetectedCategory;
    }

    public void setOriginalDetectedCategory(String originalDetectedCategory) {
        this.originalDetectedCategory = originalDetectedCategory;
    }

    public LocalDateTime getCategoryCorrectedAt() {
        return categoryCorrectedAt;
    }

    public void setCategoryCorrectedAt(LocalDateTime categoryCorrectedAt) {
        this.categoryCorrectedAt = categoryCorrectedAt;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public String getIdempotencyKey() {
        return idempotencyKey;
    }

    public void setIdempotencyKey(String idempotencyKey) {
        this.idempotencyKey = idempotencyKey;
    }

    public String getReceiverAccountNumber() {
        return receiverAccountNumber;
    }

    public void setReceiverAccountNumber(String receiverAccountNumber) {
        this.receiverAccountNumber = receiverAccountNumber;
    }

    public BigDecimal getBalanceAfter() {
        return balanceAfter;
    }

    public void setBalanceAfter(BigDecimal balanceAfter) {
        this.balanceAfter = balanceAfter;
    }

    public Account getAccount() {
        return account;
    }

    public void setAccount(Account account) {
        this.account = account;
    }
}
