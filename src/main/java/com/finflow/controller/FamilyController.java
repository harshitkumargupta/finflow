package com.finflow.controller;

import com.finflow.dto.*;
import com.finflow.service.AccountService;
import com.finflow.service.FamilyService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/family")
public class FamilyController {
    private final FamilyService familyService;
    private final AccountService accountService;

    public FamilyController(FamilyService familyService, AccountService accountService) {
        this.familyService = familyService;
        this.accountService = accountService;
    }

    @PostMapping
    public FamilyResponse createOrGetFamily() {
        return familyService.createOrGetFamily();
    }

    @GetMapping
    public FamilyResponse family() {
        return familyService.getFamily();
    }

    @GetMapping("/members")
    public List<FamilyMemberResponse> members() {
        return familyService.listMembers();
    }

    @PostMapping("/members")
    public ResponseEntity<FamilyMemberResponse> createMember(
            @Valid @RequestBody FamilyMemberRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(familyService.createMember(request));
    }

    @GetMapping("/members/{memberId}")
    public FamilyMemberResponse member(@PathVariable Long memberId) {
        return familyService.getMember(memberId);
    }

    @PutMapping("/members/{memberId}")
    public FamilyMemberResponse updateMember(
            @PathVariable Long memberId,
            @Valid @RequestBody FamilyMemberRequest request) {
        return familyService.updateMember(memberId, request);
    }

    @PutMapping("/members/{memberId}/freeze")
    public FamilyMemberResponse freeze(@PathVariable Long memberId) {
        return familyService.setFrozen(memberId, true);
    }

    @PutMapping("/members/{memberId}/unfreeze")
    public FamilyMemberResponse unfreeze(@PathVariable Long memberId) {
        return familyService.setFrozen(memberId, false);
    }

    @PostMapping("/members/{memberId}/allowances")
    public ResponseEntity<FamilyWalletTransactionResponse> allowance(
            @PathVariable Long memberId,
            @RequestHeader(value = "Idempotency-Key", required = false) String idempotencyKey,
            @Valid @RequestBody FamilyAllowanceRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(familyService.addAllowance(memberId, request, idempotencyKey));
    }

    @PostMapping("/members/{memberId}/spends")
    public TransactionResponse spend(
            @PathVariable Long memberId,
            @RequestHeader(value = "Idempotency-Key", required = false) String idempotencyKey,
            @Valid @RequestBody FamilySpendRequest request) {
        return accountService.spendFamilyAllowance(
                request.accountId(), memberId, request, idempotencyKey);
    }

    @GetMapping("/members/{memberId}/transactions")
    public List<FamilyWalletTransactionResponse> transactions(@PathVariable Long memberId) {
        return familyService.transactions(memberId);
    }

    @PostMapping("/members/{memberId}/category-limits")
    public FamilyCategoryLimitResponse upsertCategoryLimit(
            @PathVariable Long memberId,
            @Valid @RequestBody FamilyCategoryLimitRequest request) {
        return familyService.upsertCategoryLimit(memberId, request);
    }

    @DeleteMapping("/members/{memberId}/category-limits/{limitId}")
    public ResponseEntity<Void> deleteCategoryLimit(
            @PathVariable Long memberId,
            @PathVariable Long limitId) {
        familyService.deleteCategoryLimit(memberId, limitId);
        return ResponseEntity.noContent().build();
    }

    @DeleteMapping("/members/{memberId}")
    public ResponseEntity<Void> deleteMember(@PathVariable Long memberId) {
        familyService.deleteMember(memberId);
        return ResponseEntity.noContent().build();
    }
}
