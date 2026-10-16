package com.finflow.repository;

import com.finflow.entity.AutopayAuthorization;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface AutopayAuthorizationRepository extends JpaRepository<AutopayAuthorization, Long> {
    List<AutopayAuthorization> findByUserIdOrderByNextPaymentDateAsc(Long userId);
    Optional<AutopayAuthorization> findByIdAndUserId(Long id, Long userId);
    List<AutopayAuthorization> findBySavingsGoalIdAndUserIdOrderByNextPaymentDateAsc(Long savingsGoalId, Long userId);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT a FROM AutopayAuthorization a WHERE a.id = :id")
    Optional<AutopayAuthorization> findByIdForUpdate(@Param("id") Long id);

    @Query(value = "SELECT * FROM autopay_authorizations WHERE status = :status AND next_payment_date <= :dueDate ORDER BY next_payment_date ASC LIMIT :batchSize FOR UPDATE SKIP LOCKED", nativeQuery = true)
    List<AutopayAuthorization> claimDue(@Param("status") String status,
                                         @Param("dueDate") java.time.LocalDate dueDate,
                                         @Param("batchSize") int batchSize);
}
