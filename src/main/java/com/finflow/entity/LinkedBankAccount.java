package com.finflow.entity;

import jakarta.persistence.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "linked_bank_accounts",
        indexes = {
                @Index(name = "idx_linked_bank_user_status", columnList = "user_id,status"),
                @Index(name = "idx_linked_bank_provider_ref", columnList = "provider_link_reference")
        },
        uniqueConstraints = @UniqueConstraint(name = "uk_linked_bank_provider_ref",
                columnNames = "provider_link_reference"))
public class LinkedBankAccount {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Column(name = "bank_name", nullable = false, length = 120)
    private String bankName;

    @Column(name = "bank_code", nullable = false, length = 40)
    private String bankCode;

    @Column(name = "account_holder_name", nullable = false, length = 120)
    private String accountHolderName;

    @Column(name = "masked_account_number", nullable = false, length = 40)
    private String maskedAccountNumber;

    @Column(name = "account_type", nullable = false, length = 30)
    private String accountType;

    @Column(name = "provider_name", nullable = false, length = 80)
    private String providerName;

    @Column(name = "provider_link_reference", nullable = false, length = 120)
    private String providerLinkReference;

    @Column(name = "provider_account_reference", length = 120)
    private String providerAccountReference;

    @Column(nullable = false, length = 24)
    private String status;

    @Column(name = "verification_hint", length = 180)
    private String verificationHint;

    @Column(name = "failure_reason", length = 255)
    private String failureReason;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    @Column(name = "verified_at")
    private LocalDateTime verifiedAt;

    @Column(name = "disconnected_at")
    private LocalDateTime disconnectedAt;

    public Long getId() { return id; }
    public User getUser() { return user; }
    public void setUser(User user) { this.user = user; }
    public String getBankName() { return bankName; }
    public void setBankName(String bankName) { this.bankName = bankName; }
    public String getBankCode() { return bankCode; }
    public void setBankCode(String bankCode) { this.bankCode = bankCode; }
    public String getAccountHolderName() { return accountHolderName; }
    public void setAccountHolderName(String accountHolderName) { this.accountHolderName = accountHolderName; }
    public String getMaskedAccountNumber() { return maskedAccountNumber; }
    public void setMaskedAccountNumber(String maskedAccountNumber) { this.maskedAccountNumber = maskedAccountNumber; }
    public String getAccountType() { return accountType; }
    public void setAccountType(String accountType) { this.accountType = accountType; }
    public String getProviderName() { return providerName; }
    public void setProviderName(String providerName) { this.providerName = providerName; }
    public String getProviderLinkReference() { return providerLinkReference; }
    public void setProviderLinkReference(String providerLinkReference) { this.providerLinkReference = providerLinkReference; }
    public String getProviderAccountReference() { return providerAccountReference; }
    public void setProviderAccountReference(String providerAccountReference) { this.providerAccountReference = providerAccountReference; }
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
    public String getVerificationHint() { return verificationHint; }
    public void setVerificationHint(String verificationHint) { this.verificationHint = verificationHint; }
    public String getFailureReason() { return failureReason; }
    public void setFailureReason(String failureReason) { this.failureReason = failureReason; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
    public LocalDateTime getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(LocalDateTime updatedAt) { this.updatedAt = updatedAt; }
    public LocalDateTime getVerifiedAt() { return verifiedAt; }
    public void setVerifiedAt(LocalDateTime verifiedAt) { this.verifiedAt = verifiedAt; }
    public LocalDateTime getDisconnectedAt() { return disconnectedAt; }
    public void setDisconnectedAt(LocalDateTime disconnectedAt) { this.disconnectedAt = disconnectedAt; }
}
