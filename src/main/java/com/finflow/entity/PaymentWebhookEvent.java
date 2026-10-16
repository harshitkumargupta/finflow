package com.finflow.entity;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "payment_webhook_events", uniqueConstraints = @UniqueConstraint(
        name = "uk_webhook_provider_event", columnNames = {"provider", "event_id"}))
public class PaymentWebhookEvent {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 50)
    private String provider;

    @Column(name = "event_id", nullable = false, length = 100)
    private String eventId;

    @Column(name = "event_type", length = 50)
    private String eventType;

    @Column(columnDefinition = "TEXT")
    private String payload;

    @Column(nullable = false, length = 30)
    private String status;

    @Column(name = "received_at", nullable = false)
    private LocalDateTime receivedAt;

    @Column(name = "processed_at")
    private LocalDateTime processedAt;

    public PaymentWebhookEvent() {
    }

    public PaymentWebhookEvent(String provider, String eventId, String eventType, String payload, String status, LocalDateTime receivedAt) {
        this.provider = provider;
        this.eventId = eventId;
        this.eventType = eventType;
        this.payload = payload;
        this.status = status;
        this.receivedAt = receivedAt;
    }

    public Long getId() {
        return id;
    }

    public String getProvider() {
        return provider;
    }

    public void setProvider(String provider) {
        this.provider = provider;
    }

    public String getEventId() {
        return eventId;
    }

    public void setEventId(String eventId) {
        this.eventId = eventId;
    }

    public String getEventType() {
        return eventType;
    }

    public void setEventType(String eventType) {
        this.eventType = eventType;
    }

    public String getPayload() {
        return payload;
    }

    public void setPayload(String payload) {
        this.payload = payload;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public LocalDateTime getReceivedAt() {
        return receivedAt;
    }

    public void setReceivedAt(LocalDateTime receivedAt) {
        this.receivedAt = receivedAt;
    }

    public LocalDateTime getProcessedAt() {
        return processedAt;
    }

    public void setProcessedAt(LocalDateTime processedAt) {
        this.processedAt = processedAt;
    }
}
