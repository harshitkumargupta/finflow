package com.finflow.entity;

import jakarta.persistence.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "family_members",
        indexes = @Index(name = "idx_family_member_parent_status", columnList = "parent_user_id,status"))
public class FamilyMember {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "family_id", nullable = false)
    private Family family;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "parent_user_id", nullable = false)
    private User parent;

    @Column(name = "display_name", nullable = false, length = 60)
    private String displayName;

    @Column(name = "monthly_allowance", nullable = false, precision = 19, scale = 2)
    private BigDecimal monthlyAllowance;

    @Column(name = "approval_required", nullable = false)
    private boolean approvalRequired;

    @Column(nullable = false, length = 20)
    private String status;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    public Long getId() { return id; }
    public Family getFamily() { return family; }
    public void setFamily(Family family) { this.family = family; }
    public User getParent() { return parent; }
    public void setParent(User parent) { this.parent = parent; }
    public String getDisplayName() { return displayName; }
    public void setDisplayName(String displayName) { this.displayName = displayName; }
    public BigDecimal getMonthlyAllowance() { return monthlyAllowance; }
    public void setMonthlyAllowance(BigDecimal monthlyAllowance) { this.monthlyAllowance = monthlyAllowance; }
    public boolean isApprovalRequired() { return approvalRequired; }
    public void setApprovalRequired(boolean approvalRequired) { this.approvalRequired = approvalRequired; }
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
    public LocalDateTime getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(LocalDateTime updatedAt) { this.updatedAt = updatedAt; }
}
