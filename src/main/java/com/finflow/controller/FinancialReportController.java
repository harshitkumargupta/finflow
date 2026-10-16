package com.finflow.controller;

import com.finflow.dto.FinancialReportResponse;
import com.finflow.service.FinancialReportService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/reports")
public class FinancialReportController {
    private final FinancialReportService reportService;

    public FinancialReportController(FinancialReportService reportService) {
        this.reportService = reportService;
    }

    @GetMapping
    public FinancialReportResponse getReport() { return reportService.getReport(); }
}
