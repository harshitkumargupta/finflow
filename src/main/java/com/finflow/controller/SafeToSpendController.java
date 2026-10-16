package com.finflow.controller;

import com.finflow.dto.SafeToSpendResponse;
import com.finflow.dto.SafeToSpendChangeResponse;
import com.finflow.service.SafeToSpendChangeService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/insights")
public class SafeToSpendController {
    private final SafeToSpendChangeService changeService;

    public SafeToSpendController(SafeToSpendChangeService changeService) {
        this.changeService = changeService;
    }

    @GetMapping("/safe-to-spend")
    public SafeToSpendResponse safeToSpend() {
        return changeService.currentWithSnapshot();
    }

    @GetMapping("/safe-to-spend/changes")
    public SafeToSpendChangeResponse safeToSpendChanges() {
        return changeService.explainChange();
    }
}
