package com.finflow.entity;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "security_audit_logs")
public class SecurityAuditLog {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id")
    private User user;

    @Column(nullable = false, length = 60)
    private String action;

    @Column(length = 120)
    private String resource;

    @Column(length = 120)
    private String referenceId;

    @Column(length = 20)
    private String riskLevel; // LOW, MEDIUM, HIGH, BLOCKED

    @Column(nullable = false, length = 20)
    private String status; // SUCCESS, FAILED, BLOCKED, WARNING

    @Column(length = 255)
    private String details;

    @Column(length = 60)
    private String ipAddress;

    @Column(nullable = false)
    private LocalDateTime createdAt;

    public SecurityAuditLog() {}

    public SecurityAuditLog(User user, String action, String resource, String referenceId, String riskLevel, String status, String details, String ipAddress, LocalDateTime createdAt) {
        this.user = user;
        this.action = action;
        this.resource = resource;
        this.referenceId = referenceId;
        this.riskLevel = riskLevel;
        this.status = status;
        this.details = details;
        this.ipAddress = ipAddress;
        this.createdAt = createdAt;
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public User getUser() { return user; }
    public void setUser(User user) { this.user = user; }

    public String getAction() { return action; }
    public void setAction(String action) { this.action = action; }

    public String getResource() { return resource; }
    public void setResource(String resource) { this.resource = resource; }

    public String getReferenceId() { return referenceId; }
    public void setReferenceId(String referenceId) { this.referenceId = referenceId; }

    public String getRiskLevel() { return riskLevel; }
    public void setRiskLevel(String riskLevel) { this.riskLevel = riskLevel; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }

    public String getDetails() { return details; }
    public void setDetails(String details) { this.details = details; }

    public String getIpAddress() { return ipAddress; }
    public void setIpAddress(String ipAddress) { this.ipAddress = ipAddress; }

    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
}
