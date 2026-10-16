package com.finflow.controller;

import com.finflow.service.PaymentReconciliationService;
import com.finflow.dto.ReconciliationPageResponse;
import org.springframework.format.annotation.DateTimeFormat;
import java.time.LocalDateTime;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.bind.annotation.RequestParam;

@RestController
@RequestMapping("/api/payments/reconciliation")
public class ReconciliationController {

    private final PaymentReconciliationService reconciliationService;

    public ReconciliationController(PaymentReconciliationService reconciliationService) {
        this.reconciliationService = reconciliationService;
    }

    @GetMapping
    public ReconciliationPageResponse getReconciliation(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime from,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime to,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "50") int size) {
        if (page < 0 || size < 1 || size > 100 || from.isAfter(to)) {
            throw new IllegalArgumentException("Invalid reconciliation range or page size");
        }
        return reconciliationService.runReconciliation(from, to, page, size);
    }
}
