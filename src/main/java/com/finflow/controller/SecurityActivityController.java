package com.finflow.controller;

import com.finflow.dto.SecurityAuditLogResponse;
import com.finflow.service.SecurityAuditService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/security")
public class SecurityActivityController {

    private final SecurityAuditService auditService;

    public SecurityActivityController(SecurityAuditService auditService) {
        this.auditService = auditService;
    }

    @GetMapping("/activity")
    public List<SecurityAuditLogResponse> getSecurityActivity() {
        return auditService.getRecentActivity();
    }
}
