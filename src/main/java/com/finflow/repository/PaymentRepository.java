package com.finflow.repository;

import com.finflow.entity.Payment;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import java.time.LocalDateTime;

public interface PaymentRepository extends JpaRepository<Payment, Long> {
    Page<Payment> findByCreatedAtBetweenOrderByCreatedAtDesc(LocalDateTime from, LocalDateTime to, Pageable pageable);
    Optional<Payment> findByReferenceNumber(String referenceNumber);
    Optional<Payment> findByIdempotencyKey(String idempotencyKey);
    List<Payment> findByUserIdOrderByCreatedAtDesc(Long userId);
    List<Payment> findByUserIdAndStatusOrderByCreatedAtDesc(Long userId, String status);

    Page<Payment> findByUserIdOrderByCreatedAtDescIdDesc(Long userId, Pageable pageable);
}
