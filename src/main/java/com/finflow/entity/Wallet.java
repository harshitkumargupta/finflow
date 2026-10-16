package com.finflow.entity;

import jakarta.persistence.*;
import org.hibernate.annotations.Check;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "wallets",
        uniqueConstraints = {
                @UniqueConstraint(name = "uk_wallet_user", columnNames = "user_id"),
                @UniqueConstraint(name = "uk_wallet_identifier", columnNames = "wallet_identifier"),
                @UniqueConstraint(name = "uk_wallet_payment_id", columnNames = "payment_id")
        })
@Check(name = "ck_wallet_balance_non_negative", constraints = "balance >= 0")
public class Wallet {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @OneToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Column(name = "wallet_identifier", nullable = false, length = 32)
    private String walletIdentifier;

    @Column(name = "payment_id", nullable = false, length = 50)
    private String paymentId;

    @Column(nullable = false, length = 3)
    private String currency;

    @Column(nullable = false, precision = 19, scale = 2,
            columnDefinition = "numeric(19,2) default 0")
    private BigDecimal balance = BigDecimal.ZERO.setScale(2);

    @Column(nullable = false, length = 20)
    private String status;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    public Long getId() { return id; }
    public User getUser() { return user; }
    public void setUser(User user) { this.user = user; }
    public String getWalletIdentifier() { return walletIdentifier; }
    public void setWalletIdentifier(String walletIdentifier) { this.walletIdentifier = walletIdentifier; }
    public String getPaymentId() { return paymentId; }
    public void setPaymentId(String paymentId) { this.paymentId = paymentId; }
    public String getCurrency() { return currency; }
    public void setCurrency(String currency) { this.currency = currency; }
    public BigDecimal getBalance() { return balance; }
    public void setBalance(BigDecimal balance) { this.balance = balance; }
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
    public LocalDateTime getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(LocalDateTime updatedAt) { this.updatedAt = updatedAt; }
}
