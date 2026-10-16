package com.finflow.controller;

import com.finflow.service.GoalLedgerService;
import org.springframework.web.bind.annotation.RequestParam;
import com.finflow.dto.AutopayAuthorizationResponse;
import com.finflow.dto.GoalSavingsMandateRequest;
import com.finflow.dto.GoalContributionRequest;
import com.finflow.dto.GoalPlanningRequest;
import com.finflow.dto.GoalPlanningResponse;
import com.finflow.dto.SavingsGoalRequest;
import com.finflow.dto.SavingsGoalResponse;
import com.finflow.service.AutopayAuthorizationService;
import com.finflow.service.FinancialGoalPlanningService;
import com.finflow.service.SavingsGoalService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/goals")
public class SavingsGoalController {
    private final SavingsGoalService goalService;
    private final FinancialGoalPlanningService planningService;
    private final AutopayAuthorizationService autopayService;
    private final GoalLedgerService goalLedgerService;

    public SavingsGoalController(SavingsGoalService goalService,
                                 FinancialGoalPlanningService planningService,
                                 AutopayAuthorizationService autopayService,
                                 GoalLedgerService goalLedgerService) {
        this.goalLedgerService = goalLedgerService;
        this.goalService = goalService;
        this.planningService = planningService;
        this.autopayService = autopayService;
    }

    @PostMapping
    public ResponseEntity<SavingsGoalResponse> create(@Valid @RequestBody SavingsGoalRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(goalService.create(request));
    }

    @GetMapping
    public Object list(@RequestParam(required = false) Integer page, @RequestParam(required = false) Integer size) {
        return Paging.requested(page, size) ? goalService.list(Paging.of(page, size)) : goalService.list();
    }

    @GetMapping("/{id}")
    public SavingsGoalResponse get(@PathVariable Long id) { return goalService.get(id); }

    @GetMapping("/{id}/contributions")
    public Object contributions(@PathVariable Long id, @RequestParam(required = false) Integer page,
                                @RequestParam(required = false) Integer size) {
        return Paging.requested(page, size) ? goalLedgerService.history(id, Paging.of(page, size)) : goalLedgerService.history(id);
    }

    @PutMapping("/{id}")
    public SavingsGoalResponse update(@PathVariable Long id, @Valid @RequestBody SavingsGoalRequest request) {
        return goalService.update(id, request);
    }

    @PostMapping("/{id}/contributions")
    public SavingsGoalResponse contribute(@PathVariable Long id,
                                          @Valid @RequestBody GoalContributionRequest request) {
        return goalService.contribute(id, request);
    }

    @PostMapping("/{id}/savings-autopay")
    public ResponseEntity<AutopayAuthorizationResponse> createSavingsAutopay(
            @PathVariable Long id,
            @Valid @RequestBody GoalSavingsMandateRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(autopayService.createSavingsGoalMandate(id, request));
    }

    @PostMapping("/assistant/plan")
    public GoalPlanningResponse plan(@Valid @RequestBody GoalPlanningRequest request) {
        return planningService.plan(request);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        goalService.delete(id);
        return ResponseEntity.noContent().build();
    }
}
