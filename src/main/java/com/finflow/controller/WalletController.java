package com.finflow.controller;

import com.finflow.dto.PaymentIdentityRequest;
import com.finflow.dto.TransactionResponse;
import com.finflow.dto.WalletResponse;
import com.finflow.dto.WalletFundingRequest;
import com.finflow.dto.WalletTransactionResponse;
import com.finflow.dto.WalletTransferRequest;
import com.finflow.service.WalletLedgerService;
import com.finflow.service.MoneyManagementService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/wallet")
public class WalletController {
    private final MoneyManagementService moneyManagementService;
    private final WalletLedgerService walletLedgerService;

    public WalletController(
            MoneyManagementService moneyManagementService,
            WalletLedgerService walletLedgerService) {
        this.moneyManagementService = moneyManagementService;
        this.walletLedgerService = walletLedgerService;
    }

    @PostMapping
    public ResponseEntity<WalletResponse> create() {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(moneyManagementService.createWallet());
    }

    @GetMapping
    public WalletResponse get() {
        return moneyManagementService.getWallet();
    }

    @GetMapping("/transactions")
    public List<TransactionResponse> transactions() {
        return moneyManagementService.walletTransactions();
    }

    @PostMapping("/fund")
    public ResponseEntity<WalletTransactionResponse> addFunds(
            @RequestHeader(value = "Idempotency-Key", required = false) String idempotencyKey,
            @Valid @RequestBody WalletFundingRequest request) {
        return ResponseEntity.ok(walletLedgerService.addFunds(request, idempotencyKey));
    }

    @PostMapping("/transfer")
    public WalletTransactionResponse transfer(
            @RequestHeader(value = "Idempotency-Key", required = false) String idempotencyKey,
            @Valid @RequestBody WalletTransferRequest request) {
        return walletLedgerService.transfer(request, idempotencyKey);
    }

    @GetMapping("/ledger")
    public List<WalletTransactionResponse> ledger() {
        return walletLedgerService.history();
    }

    @GetMapping("/{id}")
    public WalletResponse get(@PathVariable Long id) {
        return moneyManagementService.getWallet(id);
    }

    @PutMapping("/payment-identity")
    public WalletResponse updatePaymentIdentity(@Valid @RequestBody PaymentIdentityRequest request) {
        return moneyManagementService.updatePaymentIdentity(request);
    }
}
