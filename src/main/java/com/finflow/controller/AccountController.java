package com.finflow.controller;

import com.finflow.dto.AccountResponse;
import com.finflow.dto.CreateAccountRequest;
import com.finflow.dto.MoneyRequest;
import com.finflow.dto.TransactionCategoryUpdateRequest;
import com.finflow.dto.TransactionResponse;
import com.finflow.dto.TransferRequest;
import com.finflow.service.AccountService;

import java.util.List;
import jakarta.validation.Valid;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/accounts")
public class AccountController {

    private final AccountService accountService;

    public AccountController(AccountService accountService) {
        this.accountService = accountService;
    }

    // ============================================================
    // CREATE ACCOUNT
    // ============================================================

    @PostMapping("/user/{userId}")
    public ResponseEntity<AccountResponse> createAccount(
            @PathVariable Long userId,
            @Valid @RequestBody CreateAccountRequest request) {

        AccountResponse response =
                accountService.createAccount(userId, request);

        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(response);
    }

    // ============================================================
    // GET ACCOUNT
    // ============================================================

    @GetMapping("/mine")
    public ResponseEntity<List<AccountResponse>> getMyAccounts() {
        return ResponseEntity.ok(accountService.getMyAccounts());
    }

    @GetMapping("/primary")
    public ResponseEntity<AccountResponse> getPrimaryAccount() {
        return ResponseEntity.ok(accountService.getPrimaryAccount());
    }

    @PutMapping("/{accountId}/primary")
    public ResponseEntity<AccountResponse> setPrimaryAccount(
            @PathVariable Long accountId) {
        return ResponseEntity.ok(accountService.setPrimaryAccount(accountId));
    }

    @GetMapping("/{accountId}")
    public ResponseEntity<AccountResponse> getAccount(
            @PathVariable Long accountId) {

        AccountResponse response =
                accountService.getAccount(accountId);

        return ResponseEntity.ok(response);
    }

    // ============================================================
    // DEPOSIT
    // ============================================================

    @PostMapping("/{accountId}/deposit")
    public ResponseEntity<AccountResponse> deposit(
            @PathVariable Long accountId,
            @RequestHeader(value = "Idempotency-Key", required = false) String idempotencyKey,
            @Valid @RequestBody MoneyRequest request) {

        AccountResponse response =
                accountService.deposit(accountId, request, idempotencyKey);

        return ResponseEntity.ok(response);
    }

    // ============================================================
    // WITHDRAW
    // ============================================================

    @PostMapping("/{accountId}/withdraw")
    public ResponseEntity<AccountResponse> withdraw(
            @PathVariable Long accountId,
            @RequestHeader(value = "Idempotency-Key", required = false) String idempotencyKey,
            @Valid @RequestBody MoneyRequest request) {

        AccountResponse response =
                accountService.withdraw(accountId, request, idempotencyKey);

        return ResponseEntity.ok(response);
    }

    // ============================================================
    // TRANSFER
    // ============================================================

    @PostMapping("/{accountId}/transfer")
    public ResponseEntity<AccountResponse> transfer(
            @PathVariable Long accountId,
            @RequestHeader(value = "Idempotency-Key", required = false) String idempotencyKey,
            @Valid @RequestBody TransferRequest request) {

        AccountResponse response =
                accountService.transfer(
                        accountId,
                        request,
                        idempotencyKey
                );

        return ResponseEntity.ok(response);
    }

    // ============================================================
// TRANSACTION HISTORY
// ============================================================

@GetMapping("/{accountId}/transactions")
public ResponseEntity<?> getTransactionHistory(
        @PathVariable Long accountId,
        @RequestParam(required = false) Integer page,
        @RequestParam(required = false) Integer size) {

    if (Paging.requested(page, size)) {
        var pageable = Paging.of(page, size);
        return ResponseEntity.ok(accountService.getTransactionHistory(accountId, pageable.getPageNumber(), pageable.getPageSize()));
    }

    List<TransactionResponse> transactions =
            accountService.getTransactionHistory(accountId);

    return ResponseEntity.ok(transactions);
}

@PutMapping("/{accountId}/transactions/{transactionId}/category")
public ResponseEntity<TransactionResponse> updateTransactionCategory(
        @PathVariable Long accountId,
        @PathVariable Long transactionId,
        @Valid @RequestBody TransactionCategoryUpdateRequest request) {

    return ResponseEntity.ok(accountService.updateTransactionCategory(
            accountId,
            transactionId,
            request.category()
    ));
}
}
