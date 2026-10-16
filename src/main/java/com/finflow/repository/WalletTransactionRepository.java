package com.finflow.repository;

import com.finflow.entity.WalletTransaction;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface WalletTransactionRepository extends JpaRepository<WalletTransaction, Long> {
    Optional<WalletTransaction> findByWalletIdAndIdempotencyKey(Long walletId, String idempotencyKey);

    List<WalletTransaction> findByWalletIdOrderByCreatedAtDesc(Long walletId);
}
