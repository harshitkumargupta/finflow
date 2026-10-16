package com.finflow.entity;

import jakarta.persistence.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "wallet_payments",
        uniqueConstraints = @UniqueConstraint(name = "uk_wallet_payment_sender_key",
                columnNames = {"sender_user_id", "idempotency_key"}),
        indexes = {
                @Index(name = "idx_wallet_payment_sender_created", columnList = "sender_user_id,created_at"),
                @Index(name = "idx_wallet_payment_receiver_created", columnList = "receiver_user_id,created_at")
        })
public class WalletPayment {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "sender_user_id", nullable = false)
    private User sender;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "receiver_user_id", nullable = false)
    private User receiver;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "sender_account_id", nullable = false)
    private Account senderAccount;

    @Column(name = "sender_payment_id", nullable = false, length = 50)
    private String senderPaymentId;

    @Column(name = "receiver_payment_id", nullable = false, length = 50)
    private String receiverPaymentId;

    @Column(name = "reference_number", nullable = false, unique = true, length = 40)
    private String referenceNumber;

    @Column(nullable = false, precision = 19, scale = 2)
    private BigDecimal amount;

    @Column(nullable = false, length = 20)
    private String category;

    @Column(length = 255)
    private String description;

    @Column(nullable = false, length = 24)
    private String provider;

    @Column(nullable = false, length = 20)
    private String status;

    @Column(name = "idempotency_key", nullable = false, length = 100)
    private String idempotencyKey;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    public Long getId() { return id; }
    public User getSender() { return sender; }
    public void setSender(User sender) { this.sender = sender; }
    public User getReceiver() { return receiver; }
    public void setReceiver(User receiver) { this.receiver = receiver; }
    public Account getSenderAccount() { return senderAccount; }
    public void setSenderAccount(Account senderAccount) { this.senderAccount = senderAccount; }
    public String getSenderPaymentId() { return senderPaymentId; }
    public void setSenderPaymentId(String senderPaymentId) { this.senderPaymentId = senderPaymentId; }
    public String getReceiverPaymentId() { return receiverPaymentId; }
    public void setReceiverPaymentId(String receiverPaymentId) { this.receiverPaymentId = receiverPaymentId; }
    public String getReferenceNumber() { return referenceNumber; }
    public void setReferenceNumber(String referenceNumber) { this.referenceNumber = referenceNumber; }
    public BigDecimal getAmount() { return amount; }
    public void setAmount(BigDecimal amount) { this.amount = amount; }
    public String getCategory() { return category; }
    public void setCategory(String category) { this.category = category; }
    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }
    public String getProvider() { return provider; }
    public void setProvider(String provider) { this.provider = provider; }
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
    public String getIdempotencyKey() { return idempotencyKey; }
    public void setIdempotencyKey(String idempotencyKey) { this.idempotencyKey = idempotencyKey; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
}
