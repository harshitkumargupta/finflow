package com.finflow.repository;

import com.finflow.entity.WalletPayment;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import java.time.LocalDateTime;

import java.util.List;
import java.util.Optional;

public interface WalletPaymentRepository extends JpaRepository<WalletPayment, Long> {
    Optional<WalletPayment> findBySender_IdAndIdempotencyKey(Long senderId, String idempotencyKey);
    Page<WalletPayment> findByCreatedAtBetweenOrderByCreatedAtDesc(LocalDateTime from, LocalDateTime to, Pageable pageable);

    List<WalletPayment> findBySender_IdOrReceiver_IdOrderByCreatedAtDesc(
            Long senderId, Long receiverId);

    @Query("""
            SELECT p FROM WalletPayment p
            WHERE p.id = :id AND (p.sender.id = :userId OR p.receiver.id = :userId)
            """)
    Optional<WalletPayment> findVisibleById(@Param("id") Long id, @Param("userId") Long userId);

    Page<WalletPayment> findBySender_IdOrReceiver_IdOrderByCreatedAtDescIdDesc(Long senderId, Long receiverId, Pageable pageable);
}
