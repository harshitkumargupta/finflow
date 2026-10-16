package com.finflow.repository;

import com.finflow.entity.ReconciliationRecord;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
import java.util.Optional;

public interface ReconciliationRecordRepository extends JpaRepository<ReconciliationRecord, Long> {
    Optional<ReconciliationRecord> findByPaymentReference(String paymentReference);
    List<ReconciliationRecord> findByStatusOrderByReconciledAtDesc(String status);
}
