package com.finflow.controller;

import com.finflow.dto.*;
import com.finflow.service.FinancialIntelligenceService;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/insights")
public class FinancialIntelligenceController {
    private final FinancialIntelligenceService service;
    public FinancialIntelligenceController(FinancialIntelligenceService service) { this.service = service; }
    @GetMapping("/financial-summary")
    public FinancialSummaryResponse summary() { return service.summary(); }
    @PostMapping("/purchase-plan")
    public PurchasePlanResponse purchasePlan(@Valid @RequestBody PurchasePlanRequest request) { return service.purchasePlan(request); }
}
