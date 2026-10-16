package com.finflow.controller;

import com.finflow.dto.*;
import com.finflow.service.InvestmentInformationService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/investments")
public class InvestmentController {
    private final InvestmentInformationService investmentInformationService;

    public InvestmentController(InvestmentInformationService investmentInformationService) {
        this.investmentInformationService = investmentInformationService;
    }

    @GetMapping("/education")
    public InvestmentEducationResponse education() {
        return investmentInformationService.education();
    }

    @GetMapping("/market-news")
    public MarketNewsResponse marketNews() {
        return investmentInformationService.latestNews();
    }

    @GetMapping("/market-snapshot")
    public MarketSnapshotResponse marketSnapshot() {
        return investmentInformationService.marketSnapshot();
    }

    @GetMapping("/watchlist")
    public List<WatchlistResponse> watchlist() {
        return investmentInformationService.watchlist();
    }

    @PostMapping("/watchlist")
    public ResponseEntity<WatchlistResponse> addToWatchlist(
            @Valid @RequestBody WatchlistRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(investmentInformationService.addToWatchlist(request));
    }

    @DeleteMapping("/watchlist/{id}")
    public ResponseEntity<Void> removeFromWatchlist(@PathVariable Long id) {
        investmentInformationService.removeFromWatchlist(id);
        return ResponseEntity.noContent().build();
    }
}
