package com.finflow.repository;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import com.finflow.entity.LinkedBankAccount;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface LinkedBankAccountRepository extends JpaRepository<LinkedBankAccount, Long> {
    List<LinkedBankAccount> findByUserIdOrderByCreatedAtDesc(Long userId);
    Optional<LinkedBankAccount> findByIdAndUserId(Long id, Long userId);
    boolean existsByUserIdAndBankCodeAndMaskedAccountNumberAndStatusNot(
            Long userId, String bankCode, String maskedAccountNumber, String status);

    Page<LinkedBankAccount> findByUserIdOrderByCreatedAtDescIdDesc(Long userId, Pageable pageable);
}
