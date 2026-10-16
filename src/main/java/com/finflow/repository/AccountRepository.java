package com.finflow.repository;

import com.finflow.entity.Account;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

public interface AccountRepository extends JpaRepository<Account, Long> {

    Optional<Account> findByAccountNumber(String accountNumber);

    @Query("SELECT a.id FROM Account a WHERE a.accountNumber = :accountNumber")
    Optional<Long> findIdByAccountNumber(@Param("accountNumber") String accountNumber);

    List<Account> findByUserIdOrderByCreatedAtDesc(Long userId);

    Optional<Account> findFirstByUserIdAndIsPrimaryTrue(Long userId);

    boolean existsByAccountNumber(String accountNumber);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT a FROM Account a WHERE a.id = :id")
    Optional<Account> findByIdForUpdate(@Param("id") Long id);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT a FROM Account a WHERE a.user.id = :userId ORDER BY a.id")
    List<Account> findByUserIdForUpdate(@Param("userId") Long userId);

    @Query("SELECT COALESCE(SUM(a.balance), 0) FROM Account a WHERE a.user.id = :userId")
    BigDecimal sumBalancesByUserId(@Param("userId") Long userId);
}