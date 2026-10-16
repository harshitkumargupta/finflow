package com.finflow.entity;

import jakarta.persistence.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "family_wallet_transactions",
        uniqueConstraints = @UniqueConstraint(name = "uk_family_wallet_member_key",
                columnNames = {"family_member_id", "idempotency_key"}),
        indexes = @Index(name = "idx_family_wallet_member_created",
                columnList = "family_member_id,created_at"))
public class FamilyWalletTransaction {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "family_member_id", nullable = false)
    private FamilyMember familyMember;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "parent_user_id", nullable = false)
    private User parent;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "account_id", nullable = false)
    private Account account;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "account_transaction_id")
    private Transaction accountTransaction;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "reservation_id")
    private MoneyReservation reservation;

    @Column(nullable = false, length = 20)
    private String type;

    @Column(nullable = false, precision = 19, scale = 2)
    private BigDecimal amount;

    @Column(length = 20)
    private String category;

    @Column(nullable = false, length = 100)
    private String idempotencyKey;

    @Column(length = 255)
    private String description;

    @Column(nullable = false, length = 20)
    private String status;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    public Long getId() { return id; }
    public FamilyMember getFamilyMember() { return familyMember; }
    public void setFamilyMember(FamilyMember familyMember) { this.familyMember = familyMember; }
    public User getParent() { return parent; }
    public void setParent(User parent) { this.parent = parent; }
    public Account getAccount() { return account; }
    public void setAccount(Account account) { this.account = account; }
    public Transaction getAccountTransaction() { return accountTransaction; }
    public void setAccountTransaction(Transaction accountTransaction) { this.accountTransaction = accountTransaction; }
    public MoneyReservation getReservation() { return reservation; }
    public void setReservation(MoneyReservation reservation) { this.reservation = reservation; }
    public String getType() { return type; }
    public void setType(String type) { this.type = type; }
    public BigDecimal getAmount() { return amount; }
    public void setAmount(BigDecimal amount) { this.amount = amount; }
    public String getCategory() { return category; }
    public void setCategory(String category) { this.category = category; }
    public String getIdempotencyKey() { return idempotencyKey; }
    public void setIdempotencyKey(String idempotencyKey) { this.idempotencyKey = idempotencyKey; }
    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
}
