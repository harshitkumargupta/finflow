package com.finflow.repository;

import com.finflow.entity.PaymentWebhookEvent;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface PaymentWebhookEventRepository extends JpaRepository<PaymentWebhookEvent, Long> {
    List<PaymentWebhookEvent> findByProviderOrderByReceivedAtDesc(String provider);
    boolean existsByProviderAndEventId(String provider, String eventId);
}
