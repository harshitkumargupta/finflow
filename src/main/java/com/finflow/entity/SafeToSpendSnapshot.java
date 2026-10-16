package com.finflow.entity;

import jakarta.persistence.*;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name = "safe_to_spend_snapshots", uniqueConstraints = @UniqueConstraint(
        name = "uk_safe_spend_snapshot_user_date", columnNames = {"user_id", "snapshot_date"}))
public class SafeToSpendSnapshot {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "user_id", nullable = false)
    private Long userId;

    @Column(name = "snapshot_date", nullable = false)
    private LocalDate snapshotDate;

    @Column(name = "current_balance", nullable = false, precision = 19, scale = 2)
    private BigDecimal currentBalance;

    @Column(name = "safe_to_spend", nullable = false, precision = 19, scale = 2)
    private BigDecimal safeToSpend;

    @Column(nullable = false, columnDefinition = "text")
    private String breakdown;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    public SafeToSpendSnapshot() {
    }

    public SafeToSpendSnapshot(Long userId, LocalDate snapshotDate, BigDecimal currentBalance,
                               BigDecimal safeToSpend, String breakdown, LocalDateTime createdAt) {
        this.userId = userId;
        this.snapshotDate = snapshotDate;
        this.currentBalance = currentBalance;
        this.safeToSpend = safeToSpend;
        this.breakdown = breakdown;
        this.createdAt = createdAt;
    }

    public Long getId() { return id; }
    public Long getUserId() { return userId; }
    public LocalDate getSnapshotDate() { return snapshotDate; }
    public BigDecimal getCurrentBalance() { return currentBalance; }
    public BigDecimal getSafeToSpend() { return safeToSpend; }
    public String getBreakdown() { return breakdown; }
    public LocalDateTime getCreatedAt() { return createdAt; }
}
