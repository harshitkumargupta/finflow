package com.finflow.service;

import com.finflow.dto.SecurityAuditLogResponse;
import com.finflow.entity.SecurityAuditLog;
import com.finflow.entity.User;
import com.finflow.repository.SecurityAuditLogRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Service
public class SecurityAuditService {

    private final SecurityAuditLogRepository auditLogRepository;
    private final CurrentUserService currentUserService;

    public SecurityAuditService(SecurityAuditLogRepository auditLogRepository,
                                CurrentUserService currentUserService) {
        this.auditLogRepository = auditLogRepository;
        this.currentUserService = currentUserService;
    }

    @Transactional
    public void log(User user, String action, String resource, String referenceId,
                    String riskLevel, String status, String details) {
        SecurityAuditLog log = new SecurityAuditLog(
                user,
                action,
                resource,
                referenceId,
                riskLevel != null ? riskLevel : "LOW",
                status != null ? status : "SUCCESS",
                details,
                "127.0.0.1",
                LocalDateTime.now()
        );
        auditLogRepository.save(log);
    }

    @Transactional(readOnly = true)
    public List<SecurityAuditLogResponse> getRecentActivity() {
        User user = currentUserService.get();
        return auditLogRepository.findTop50ByUserIdOrderByCreatedAtDesc(user.getId())
                .stream()
                .map(log -> new SecurityAuditLogResponse(
                        log.getId(),
                        log.getAction(),
                        log.getResource(),
                        log.getReferenceId(),
                        log.getRiskLevel(),
                        log.getStatus(),
                        log.getDetails(),
                        log.getIpAddress(),
                        log.getCreatedAt()
                )).toList();
    }
}
