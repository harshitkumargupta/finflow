package com.finflow.controller;

import com.finflow.dto.*;
import com.finflow.service.MoneyManagementService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api")
public class MoneyManagementController {
    private final MoneyManagementService moneyManagementService;

    public MoneyManagementController(MoneyManagementService moneyManagementService) {
        this.moneyManagementService = moneyManagementService;
    }

    @GetMapping("/money/safety")
    public FinancialSafetyResponse safety() {
        return moneyManagementService.safetySummary();
    }

    @GetMapping("/money/reservations")
    public List<MoneyReservationResponse> reservations() {
        return moneyManagementService.listReservations();
    }

    @PostMapping("/money/reservations")
    public ResponseEntity<MoneyReservationResponse> reserve(
            @Valid @RequestBody MoneyReservationRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(moneyManagementService.reserve(request));
    }

    @DeleteMapping("/money/reservations/{id}")
    public MoneyReservationResponse release(@PathVariable Long id) {
        return moneyManagementService.releaseReservation(id);
    }

    @GetMapping("/money-rules")
    public List<MoneyAllocationRuleResponse> rules() {
        return moneyManagementService.listRules();
    }

    @PostMapping("/money-rules")
    public ResponseEntity<MoneyAllocationRuleResponse> createRule(
            @Valid @RequestBody MoneyAllocationRuleRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(moneyManagementService.createRule(request));
    }

    @PutMapping("/money-rules/{id}")
    public MoneyAllocationRuleResponse updateRule(
            @PathVariable Long id,
            @Valid @RequestBody MoneyAllocationRuleRequest request) {
        return moneyManagementService.updateRule(id, request);
    }

    @DeleteMapping("/money-rules/{id}")
    public ResponseEntity<Void> deleteRule(@PathVariable Long id) {
        moneyManagementService.deleteRule(id);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/spending-limits")
    public List<SpendingLimitResponse> spendingLimits() {
        return moneyManagementService.listSpendingLimits();
    }

    @PostMapping("/spending-limits")
    public ResponseEntity<SpendingLimitResponse> createSpendingLimit(
            @Valid @RequestBody SpendingLimitRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(moneyManagementService.createSpendingLimit(request));
    }

    @PutMapping("/spending-limits/{id}")
    public SpendingLimitResponse updateSpendingLimit(
            @PathVariable Long id,
            @Valid @RequestBody SpendingLimitRequest request) {
        return moneyManagementService.updateSpendingLimit(id, request);
    }

    @DeleteMapping("/spending-limits/{id}")
    public ResponseEntity<Void> deleteSpendingLimit(@PathVariable Long id) {
        moneyManagementService.deleteSpendingLimit(id);
        return ResponseEntity.noContent().build();
    }
}
