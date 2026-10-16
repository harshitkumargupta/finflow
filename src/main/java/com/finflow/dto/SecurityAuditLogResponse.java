package com.finflow.dto;

import java.time.LocalDateTime;

public record SecurityAuditLogResponse(
        Long id,
        String action,
        String resource,
        String referenceId,
        String riskLevel,
        String status,
        String details,
        String ipAddress,
        LocalDateTime createdAt
) {}
