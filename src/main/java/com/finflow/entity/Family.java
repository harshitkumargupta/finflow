package com.finflow.entity;

import jakarta.persistence.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "families",
        uniqueConstraints = @UniqueConstraint(name = "uk_family_parent", columnNames = "parent_user_id"))
public class Family {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @OneToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "parent_user_id", nullable = false)
    private User parent;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    public Long getId() { return id; }
    public User getParent() { return parent; }
    public void setParent(User parent) { this.parent = parent; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
}
