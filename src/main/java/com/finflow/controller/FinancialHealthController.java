package com.finflow.controller;

import com.finflow.dto.FinancialHealthResponse;
import com.finflow.service.FinancialHealthService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/financial-health")
public class FinancialHealthController {
    private final FinancialHealthService financialHealthService;

    public FinancialHealthController(FinancialHealthService financialHealthService) {
        this.financialHealthService = financialHealthService;
    }

    @GetMapping
    public FinancialHealthResponse getScore() { return financialHealthService.getScore(); }
}
