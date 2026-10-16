package com.finflow.controller;

import com.finflow.dto.BudgetRequest;
import com.finflow.dto.BudgetResponse;
import com.finflow.service.BudgetService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/budgets")
public class BudgetController {
    private final BudgetService budgetService;

    public BudgetController(BudgetService budgetService) {
        this.budgetService = budgetService;
    }

    @PostMapping
    public ResponseEntity<BudgetResponse> create(@Valid @RequestBody BudgetRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(budgetService.create(request));
    }

    @GetMapping
    public List<BudgetResponse> list() { return budgetService.list(); }

    @GetMapping("/{id}")
    public BudgetResponse get(@PathVariable Long id) { return budgetService.get(id); }

    @PutMapping("/{id}")
    public BudgetResponse update(@PathVariable Long id, @Valid @RequestBody BudgetRequest request) {
        return budgetService.update(id, request);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        budgetService.delete(id);
        return ResponseEntity.noContent().build();
    }
}
