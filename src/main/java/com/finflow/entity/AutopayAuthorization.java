package com.finflow.entity;

import jakarta.persistence.*;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name = "autopay_authorizations",
        indexes = @Index(name = "idx_autopay_user_status_due",
                columnList = "user_id,status,next_payment_date"))
public class AutopayAuthorization {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "account_id", nullable = false)
    private Account account;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "card_id")
    private Card card;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "savings_goal_id")
    private SavingsGoal savingsGoal;

    @Column(name = "merchant_name", nullable = false, length = 120)
    private String merchantName;

    @Column(nullable = false, length = 40)
    private String provider;

    @Column(name = "last_payment_date")
    private LocalDate lastPaymentDate;

    @Column(name = "last_payment_status", length = 30)
    private String lastPaymentStatus;

    @Column(name = "token_reference", length = 160)
    private String tokenReference;

    @Column(nullable = false, precision = 19, scale = 2)
    private BigDecimal amount;

    @Column(name = "maximum_amount", nullable = false, precision = 19, scale = 2)
    private BigDecimal maximumAmount;

    @Column(nullable = false, length = 20)
    private String frequency;

    @Column(name = "next_payment_date", nullable = false)
    private LocalDate nextPaymentDate;

    @Column(nullable = false, length = 20)
    private String status;

    @Column(length = 255)
    private String description;

    @Column(name = "provider_authorization_reference", length = 160)
    private String providerAuthorizationReference;

    @Column(name = "reservation_id")
    private Long reservationId;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    @Column(name = "cancelled_at")
    private LocalDateTime cancelledAt;

    @Column(name = "source_type", nullable = false, length = 20)
    private String sourceType = "ACCOUNT";

    @Column(length = 30)
    private String category = "BILLS";

    @Column(name = "start_date")
    private LocalDate startDate;

    @Column(name = "end_date")
    private LocalDate endDate;

    @Column(name = "retry_count", nullable = false)
    private int retryCount = 0;

    @Column(name = "max_retries", nullable = false)
    private int maxRetries = 2;

    @Column(name = "retry_policy", nullable = false, length = 30)
    private String retryPolicy = "RETRY_NEXT_CYCLE";

    @Column(name = "notify_on_success", nullable = false)
    private boolean notifyOnSuccess = true;

    @Column(name = "notify_on_failure", nullable = false)
    private boolean notifyOnFailure = true;

    public Card getCard() { return card; }
    public void setCard(Card card) { this.card = card; }
    public SavingsGoal getSavingsGoal() { return savingsGoal; }
    public void setSavingsGoal(SavingsGoal savingsGoal) { this.savingsGoal = savingsGoal; }
    public LocalDate getLastPaymentDate() { return lastPaymentDate; }
    public void setLastPaymentDate(LocalDate lastPaymentDate) { this.lastPaymentDate = lastPaymentDate; }
    public String getLastPaymentStatus() { return lastPaymentStatus; }
    public void setLastPaymentStatus(String lastPaymentStatus) { this.lastPaymentStatus = lastPaymentStatus; }
    public Long getId() { return id; }
    public User getUser() { return user; }
    public void setUser(User user) { this.user = user; }
    public Account getAccount() { return account; }
    public void setAccount(Account account) { this.account = account; }
    public String getMerchantName() { return merchantName; }
    public void setMerchantName(String merchantName) { this.merchantName = merchantName; }
    public String getProvider() { return provider; }
    public void setProvider(String provider) { this.provider = provider; }
    public String getTokenReference() { return tokenReference; }
    public void setTokenReference(String tokenReference) { this.tokenReference = tokenReference; }
    public BigDecimal getAmount() { return amount; }
    public void setAmount(BigDecimal amount) { this.amount = amount; }
    public BigDecimal getMaximumAmount() { return maximumAmount; }
    public void setMaximumAmount(BigDecimal maximumAmount) { this.maximumAmount = maximumAmount; }
    public String getFrequency() { return frequency; }
    public void setFrequency(String frequency) { this.frequency = frequency; }
    public LocalDate getNextPaymentDate() { return nextPaymentDate; }
    public void setNextPaymentDate(LocalDate nextPaymentDate) { this.nextPaymentDate = nextPaymentDate; }
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }
    public String getProviderAuthorizationReference() { return providerAuthorizationReference; }
    public void setProviderAuthorizationReference(String providerAuthorizationReference) { this.providerAuthorizationReference = providerAuthorizationReference; }
    public Long getReservationId() { return reservationId; }
    public void setReservationId(Long reservationId) { this.reservationId = reservationId; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
    public LocalDateTime getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(LocalDateTime updatedAt) { this.updatedAt = updatedAt; }
    public LocalDateTime getCancelledAt() { return cancelledAt; }
    public void setCancelledAt(LocalDateTime cancelledAt) { this.cancelledAt = cancelledAt; }
    public String getSourceType() { return sourceType; }
    public void setSourceType(String sourceType) { this.sourceType = sourceType; }
    public String getCategory() { return category; }
    public void setCategory(String category) { this.category = category; }
    public LocalDate getStartDate() { return startDate; }
    public void setStartDate(LocalDate startDate) { this.startDate = startDate; }
    public LocalDate getEndDate() { return endDate; }
    public void setEndDate(LocalDate endDate) { this.endDate = endDate; }
    public int getRetryCount() { return retryCount; }
    public void setRetryCount(int retryCount) { this.retryCount = retryCount; }
    public int getMaxRetries() { return maxRetries; }
    public void setMaxRetries(int maxRetries) { this.maxRetries = maxRetries; }
    public String getRetryPolicy() { return retryPolicy; }
    public void setRetryPolicy(String retryPolicy) { this.retryPolicy = retryPolicy; }
    public boolean isNotifyOnSuccess() { return notifyOnSuccess; }
    public void setNotifyOnSuccess(boolean notifyOnSuccess) { this.notifyOnSuccess = notifyOnSuccess; }
    public boolean isNotifyOnFailure() { return notifyOnFailure; }
    public void setNotifyOnFailure(boolean notifyOnFailure) { this.notifyOnFailure = notifyOnFailure; }
}
