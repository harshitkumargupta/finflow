package com.finflow.controller;

import com.finflow.dto.PortfolioHoldingRequest;
import com.finflow.dto.PortfolioSummaryResponse;
import com.finflow.service.PortfolioService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/investments/portfolio")
public class PortfolioController {

    private final PortfolioService portfolioService;

    public PortfolioController(PortfolioService portfolioService) {
        this.portfolioService = portfolioService;
    }

    @GetMapping
    public PortfolioSummaryResponse summary() {
        return portfolioService.summary();
    }

    @PostMapping
    public ResponseEntity<PortfolioSummaryResponse> create(@Valid @RequestBody PortfolioHoldingRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(portfolioService.create(request));
    }

    @PutMapping("/{id}")
    public PortfolioSummaryResponse update(@PathVariable Long id, @Valid @RequestBody PortfolioHoldingRequest request) {
        return portfolioService.update(id, request);
    }

    @DeleteMapping("/{id}")
    public PortfolioSummaryResponse delete(@PathVariable Long id) {
        return portfolioService.delete(id);
    }
}
