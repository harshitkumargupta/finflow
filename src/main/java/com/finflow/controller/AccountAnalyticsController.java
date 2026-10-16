package com.finflow.controller;

import com.finflow.dto.AnalyticsSummaryResponse;
import com.finflow.dto.CategoryAnalyticsResponse;
import com.finflow.dto.MonthlyAnalyticsResponse;
import com.finflow.service.AnalyticsService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/accounts/{accountId}/analytics")
public class AccountAnalyticsController {

    private final AnalyticsService analyticsService;

    public AccountAnalyticsController(AnalyticsService analyticsService) {
        this.analyticsService = analyticsService;
    }

    @GetMapping("/summary")
    public ResponseEntity<AnalyticsSummaryResponse> getSummary(
            @PathVariable Long accountId) {
        return ResponseEntity.ok(analyticsService.getSummary(accountId));
    }

    @GetMapping("/categories")
    public ResponseEntity<List<CategoryAnalyticsResponse>> getCategories(
            @PathVariable Long accountId) {
        return ResponseEntity.ok(analyticsService.getCategories(accountId));
    }

    @GetMapping("/monthly")
    public ResponseEntity<List<MonthlyAnalyticsResponse>> getMonthly(
            @PathVariable Long accountId) {
        return ResponseEntity.ok(analyticsService.getMonthly(accountId));
    }
}
