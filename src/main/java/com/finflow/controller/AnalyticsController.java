package com.finflow.controller;

import com.finflow.dto.AnalyticsSummaryResponse;
import com.finflow.dto.CategoryAnalyticsResponse;
import com.finflow.dto.MonthlyAnalyticsResponse;
import com.finflow.service.AnalyticsService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/analytics")
public class AnalyticsController {

    private final AnalyticsService analyticsService;

    public AnalyticsController(AnalyticsService analyticsService) {
        this.analyticsService = analyticsService;
    }

    @GetMapping("/summary")
    public ResponseEntity<AnalyticsSummaryResponse> getSummary() {
        return ResponseEntity.ok(analyticsService.getSummary());
    }

    @GetMapping("/categories")
    public ResponseEntity<List<CategoryAnalyticsResponse>> getCategories() {
        return ResponseEntity.ok(analyticsService.getCategoryAnalytics());
    }

    @GetMapping("/monthly")
    public ResponseEntity<List<MonthlyAnalyticsResponse>> getMonthly() {
        return ResponseEntity.ok(analyticsService.getMonthlyAnalytics());
    }
}
