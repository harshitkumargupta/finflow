package com.finflow.entity;

import jakarta.persistence.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "goal_contributions")
public class GoalContribution {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "goal_id", nullable = false)
    private SavingsGoal goal;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Column(nullable = false, precision = 19, scale = 2)
    private BigDecimal amount;

    @Column(name = "goal_amount_after", nullable = false, precision = 19, scale = 2)
    private BigDecimal goalAmountAfter;

    @Column(nullable = false, length = 20)
    private String source;

    @Column(length = 120)
    private String reference;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    public GoalContribution() {
    }

    public GoalContribution(SavingsGoal goal, User user, BigDecimal amount, BigDecimal goalAmountAfter,
                            String source, String reference, LocalDateTime createdAt) {
        this.goal = goal;
        this.user = user;
        this.amount = amount;
        this.goalAmountAfter = goalAmountAfter;
        this.source = source;
        this.reference = reference;
        this.createdAt = createdAt;
    }

    public Long getId() { return id; }
    public SavingsGoal getGoal() { return goal; }
    public User getUser() { return user; }
    public BigDecimal getAmount() { return amount; }
    public BigDecimal getGoalAmountAfter() { return goalAmountAfter; }
    public String getSource() { return source; }
    public String getReference() { return reference; }
    public LocalDateTime getCreatedAt() { return createdAt; }
}
