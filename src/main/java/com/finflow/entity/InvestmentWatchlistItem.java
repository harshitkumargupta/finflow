package com.finflow.entity;

import jakarta.persistence.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "investment_watchlist_items",
        uniqueConstraints = @UniqueConstraint(name = "uk_watchlist_user_symbol",
                columnNames = {"user_id", "symbol"}),
        indexes = @Index(name = "idx_watchlist_user_added", columnList = "user_id,added_at"))
public class InvestmentWatchlistItem {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Column(nullable = false, length = 24)
    private String symbol;

    @Column(nullable = false, length = 120)
    private String name;

    @Column(nullable = false, length = 30)
    private String category;

    @Column(name = "added_at", nullable = false)
    private LocalDateTime addedAt;

    public Long getId() { return id; }
    public User getUser() { return user; }
    public void setUser(User user) { this.user = user; }
    public String getSymbol() { return symbol; }
    public void setSymbol(String symbol) { this.symbol = symbol; }
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public String getCategory() { return category; }
    public void setCategory(String category) { this.category = category; }
    public LocalDateTime getAddedAt() { return addedAt; }
    public void setAddedAt(LocalDateTime addedAt) { this.addedAt = addedAt; }
}
