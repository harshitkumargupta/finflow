package com.finflow.repository;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import com.finflow.entity.AutopayExecution;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface AutopayExecutionRepository extends JpaRepository<AutopayExecution, Long> {
    List<AutopayExecution> findByUserIdOrderByExecutedAtDesc(Long userId);
    List<AutopayExecution> findByAutopayAuthorizationIdOrderByExecutedAtDesc(Long autopayAuthorizationId);
    List<AutopayExecution> findByAutopayAuthorizationIdAndUserIdOrderByExecutedAtDesc(Long autopayAuthorizationId, Long userId);
    boolean existsByAutopayAuthorizationIdAndStatusAndExecutedAtAfter(Long autopayAuthorizationId, String status, LocalDateTime after);
    boolean existsByIdempotencyKey(String idempotencyKey);

    Page<AutopayExecution> findByUserIdOrderByExecutedAtDescIdDesc(Long userId, Pageable pageable);

    Page<AutopayExecution> findByAutopayAuthorizationIdAndUserIdOrderByExecutedAtDescIdDesc(Long autopayAuthorizationId, Long userId, Pageable pageable);
}
