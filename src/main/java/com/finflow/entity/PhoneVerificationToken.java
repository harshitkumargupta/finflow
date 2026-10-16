package com.finflow.entity;

import jakarta.persistence.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "phone_verification_tokens")
public class PhoneVerificationToken {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Column(name = "code_hash", nullable = false, length = 64)
    private String codeHash;

    @Column(nullable = false)
    private int attempts;

    @Column(name = "expires_at", nullable = false)
    private LocalDateTime expiresAt;

    @Column(name = "used_at")
    private LocalDateTime usedAt;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    public PhoneVerificationToken() {
    }

    public PhoneVerificationToken(User user, String codeHash, LocalDateTime expiresAt, LocalDateTime createdAt) {
        this.user = user;
        this.codeHash = codeHash;
        this.expiresAt = expiresAt;
        this.createdAt = createdAt;
    }

    public Long getId() { return id; }
    public User getUser() { return user; }
    public String getCodeHash() { return codeHash; }
    public int getAttempts() { return attempts; }
    public void setAttempts(int attempts) { this.attempts = attempts; }
    public LocalDateTime getExpiresAt() { return expiresAt; }
    public LocalDateTime getUsedAt() { return usedAt; }
    public void setUsedAt(LocalDateTime usedAt) { this.usedAt = usedAt; }
    public LocalDateTime getCreatedAt() { return createdAt; }
}
