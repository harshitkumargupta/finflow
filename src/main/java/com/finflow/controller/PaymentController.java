package com.finflow.controller;

import org.springframework.web.bind.annotation.RequestParam;
import com.finflow.dto.*;
import com.finflow.service.PaymentDomainService;
import jakarta.validation.Valid;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.List;

@RestController
@RequestMapping("/api/payments")
public class PaymentController {

    private final PaymentDomainService paymentService;

    public PaymentController(PaymentDomainService paymentService) {
        this.paymentService = paymentService;
    }

    @PostMapping("/resolve-recipient")
    public ResponseEntity<RecipientResolutionResponse> resolveRecipient(
            @Valid @RequestBody RecipientResolutionRequest request) {
        return ResponseEntity.ok(paymentService.resolveRecipient(request));
    }

    @PostMapping("/qr/generate")
    public ResponseEntity<QRGenerateResponse> generateQR(
            @RequestBody QRGenerateRequest request) {
        return ResponseEntity.ok(paymentService.generateQR(request));
    }

    @PostMapping("/qr/decode")
    public ResponseEntity<RecipientResolutionResponse> decodeQR(
            @Valid @RequestBody QRDecodeRequest request) {
        return ResponseEntity.ok(paymentService.decodeQR(request));
    }

    @PostMapping("/execute")
    public ResponseEntity<PaymentDomainResponse> executePayment(
            @RequestHeader(value = "Idempotency-Key", required = false) String idempotencyHeader,
            @Valid @RequestBody PaymentExecutionRequest request) {

        String key = (request.idempotencyKey() != null && !request.idempotencyKey().isBlank())
                ? request.idempotencyKey()
                : idempotencyHeader;

        PaymentExecutionRequest reqWithKey = new PaymentExecutionRequest(
                request.sourceType(),
                request.sourceId(),
                request.recipientType(),
                request.recipientIdentifier(),
                request.recipientName(),
                request.amount(),
                request.category(),
                request.description(),
                key,
                request.pin()
        );

        PaymentDomainResponse response = paymentService.executePayment(reqWithKey);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @GetMapping("/mine")
    public ResponseEntity<?> getMyPayments(@RequestParam(required = false) Integer page, @RequestParam(required = false) Integer size) {
        if (Paging.requested(page, size)) return ResponseEntity.ok(paymentService.listMyPayments(Paging.of(page, size)));
        return ResponseEntity.ok(paymentService.listMyPayments());
    }

    @GetMapping("/{id}")
    public ResponseEntity<PaymentDomainResponse> getPayment(@PathVariable Long id) {
        return ResponseEntity.ok(paymentService.getPayment(id));
    }

    @PostMapping("/reconciliation/run")
    public ResponseEntity<ReconciliationPageResponse> runReconciliation(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime from,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime to,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "50") int size) {
        if (page < 0 || size < 1 || size > 100 || from.isAfter(to)) {
            throw new IllegalArgumentException("Invalid reconciliation range or page size");
        }
        return ResponseEntity.ok(paymentService.runReconciliation(from, to, page, size));
    }
}
