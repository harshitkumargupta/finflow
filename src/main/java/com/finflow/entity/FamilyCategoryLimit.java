package com.finflow.entity;

import jakarta.persistence.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "family_category_limits",
        uniqueConstraints = @UniqueConstraint(name = "uk_family_member_category",
                columnNames = {"family_member_id", "category"}))
public class FamilyCategoryLimit {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "family_member_id", nullable = false)
    private FamilyMember familyMember;

    @Column(nullable = false, length = 20)
    private String category;

    @Column(name = "monthly_amount", nullable = false, precision = 19, scale = 2)
    private BigDecimal monthlyAmount;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    public Long getId() { return id; }
    public FamilyMember getFamilyMember() { return familyMember; }
    public void setFamilyMember(FamilyMember familyMember) { this.familyMember = familyMember; }
    public String getCategory() { return category; }
    public void setCategory(String category) { this.category = category; }
    public BigDecimal getMonthlyAmount() { return monthlyAmount; }
    public void setMonthlyAmount(BigDecimal monthlyAmount) { this.monthlyAmount = monthlyAmount; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
}
