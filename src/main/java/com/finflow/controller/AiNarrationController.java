package com.finflow.controller;

import com.finflow.dto.CategorizeRequest;
import com.finflow.dto.CategorySuggestionResponse;
import com.finflow.dto.InsightSummaryResponse;
import com.finflow.dto.SpendingWarningsResponse;
import com.finflow.service.CurrentUserService;
import com.finflow.service.InsightNarrationService;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/insights/narration")
public class AiNarrationController {

    private final InsightNarrationService narrationService;
    private final CurrentUserService currentUserService;

    public AiNarrationController(InsightNarrationService narrationService, CurrentUserService currentUserService) {
        this.narrationService = narrationService;
        this.currentUserService = currentUserService;
    }

    @PostMapping("/categorize")
    public CategorySuggestionResponse categorize(@Valid @RequestBody CategorizeRequest request) {
        currentUserService.get();
        return narrationService.categorize(request.description(), request.type());
    }

    @GetMapping("/spending-warnings")
    public SpendingWarningsResponse spendingWarnings() {
        currentUserService.get();
        return narrationService.spendingWarnings();
    }

    @GetMapping("/summary")
    public InsightSummaryResponse summary() {
        currentUserService.get();
        return narrationService.summary();
    }
}
