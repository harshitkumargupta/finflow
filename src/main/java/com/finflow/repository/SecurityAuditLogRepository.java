package com.finflow.repository;

import com.finflow.entity.SecurityAuditLog;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface SecurityAuditLogRepository extends JpaRepository<SecurityAuditLog, Long> {
    List<SecurityAuditLog> findByUserIdOrderByCreatedAtDesc(Long userId);
    List<SecurityAuditLog> findTop50ByUserIdOrderByCreatedAtDesc(Long userId);
}
