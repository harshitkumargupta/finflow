package com.finflow.controller;

import org.springframework.web.bind.annotation.RequestParam;
import com.finflow.dto.*;
import com.finflow.service.WalletPaymentService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/payments")
public class WalletPaymentController {
    private final WalletPaymentService walletPaymentService;

    public WalletPaymentController(WalletPaymentService walletPaymentService) {
        this.walletPaymentService = walletPaymentService;
    }

    @PostMapping("/qr/create")
    public ResponseEntity<PaymentQrResponse> createQr() {
        return ResponseEntity.status(HttpStatus.CREATED).body(walletPaymentService.createQr());
    }

    @PostMapping("/qr/scan")
    public PaymentQrScanResponse scan(@Valid @RequestBody PaymentQrScanRequest request) {
        return walletPaymentService.scan(request);
    }

    @PostMapping("/resolve")
    public PaymentQrScanResponse resolve(@Valid @RequestBody PaymentResolveRequest request) {
        return walletPaymentService.resolve(request.paymentId());
    }

    @PostMapping("/confirm")
    public ResponseEntity<WalletPaymentResponse> confirm(
            @RequestHeader(value = "Idempotency-Key", required = false) String idempotencyKey,
            @Valid @RequestBody PaymentConfirmRequest request) {
        return ResponseEntity.ok(walletPaymentService.confirm(request, idempotencyKey));
    }

    @GetMapping("/history")
    public Object history(@RequestParam(required = false) Integer page, @RequestParam(required = false) Integer size) {
        if (Paging.requested(page, size)) return walletPaymentService.history(Paging.of(page, size));
        return walletPaymentService.history();
    }

    @GetMapping("/wallet/{id}")
    public WalletPaymentResponse get(@PathVariable Long id) {
        return walletPaymentService.get(id);
    }
}
