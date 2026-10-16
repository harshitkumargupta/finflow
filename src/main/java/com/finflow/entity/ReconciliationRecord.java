package com.finflow.entity;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "reconciliation_records")
public class ReconciliationRecord {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "payment_reference", nullable = false, length = 60)
    private String paymentReference;

    @Column(name = "external_reference", length = 120)
    private String externalReference;

    @Column(name = "internal_amount", nullable = false, precision = 19, scale = 2)
    private BigDecimal internalAmount;

    @Column(name = "external_amount", precision = 19, scale = 2)
    private BigDecimal externalAmount;

    @Column(nullable = false, length = 30)
    private String status; // MATCHED, MISMATCH, PENDING_REVIEW

    @Column(length = 255)
    private String notes;

    @Column(name = "reconciled_at", nullable = false)
    private LocalDateTime reconciledAt;

    public ReconciliationRecord() {
    }

    public ReconciliationRecord(String paymentReference, String externalReference, BigDecimal internalAmount, BigDecimal externalAmount, String status, String notes, LocalDateTime reconciledAt) {
        this.paymentReference = paymentReference;
        this.externalReference = externalReference;
        this.internalAmount = internalAmount;
        this.externalAmount = externalAmount;
        this.status = status;
        this.notes = notes;
        this.reconciledAt = reconciledAt;
    }

    public Long getId() {
        return id;
    }

    public String getPaymentReference() {
        return paymentReference;
    }

    public void setPaymentReference(String paymentReference) {
        this.paymentReference = paymentReference;
    }

    public String getExternalReference() {
        return externalReference;
    }

    public void setExternalReference(String externalReference) {
        this.externalReference = externalReference;
    }

    public BigDecimal getInternalAmount() {
        return internalAmount;
    }

    public void setInternalAmount(BigDecimal internalAmount) {
        this.internalAmount = internalAmount;
    }

    public BigDecimal getExternalAmount() {
        return externalAmount;
    }

    public void setExternalAmount(BigDecimal externalAmount) {
        this.externalAmount = externalAmount;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public String getNotes() {
        return notes;
    }

    public void setNotes(String notes) {
        this.notes = notes;
    }

    public LocalDateTime getReconciledAt() {
        return reconciledAt;
    }

    public void setReconciledAt(LocalDateTime reconciledAt) {
        this.reconciledAt = reconciledAt;
    }
}
