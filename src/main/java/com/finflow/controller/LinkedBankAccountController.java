package com.finflow.controller;

import org.springframework.web.bind.annotation.RequestParam;
import com.finflow.dto.BankLinkInitiateRequest;
import com.finflow.dto.BankLinkVerifyRequest;
import com.finflow.dto.LinkedBankAccountResponse;
import com.finflow.service.LinkedBankAccountService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/bank-links")
public class LinkedBankAccountController {
    private final LinkedBankAccountService service;

    public LinkedBankAccountController(LinkedBankAccountService service) {
        this.service = service;
    }

    @PostMapping
    public ResponseEntity<LinkedBankAccountResponse> initiate(
            @Valid @RequestBody BankLinkInitiateRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(service.initiate(request));
    }

    @GetMapping
    public Object list(@RequestParam(required = false) Integer page, @RequestParam(required = false) Integer size) {
        return Paging.requested(page, size) ? service.list(Paging.of(page, size)) : service.list();
    }

    @GetMapping("/{id}")
    public LinkedBankAccountResponse get(@PathVariable Long id) {
        return service.get(id);
    }

    @PostMapping("/{id}/verify")
    public LinkedBankAccountResponse verify(
            @PathVariable Long id,
            @Valid @RequestBody BankLinkVerifyRequest request) {
        return service.verify(id, request);
    }

    @PostMapping("/{id}/refresh-status")
    public LinkedBankAccountResponse refreshStatus(@PathVariable Long id) {
        return service.refreshStatus(id);
    }

    @PostMapping("/{id}/disconnect")
    public LinkedBankAccountResponse disconnect(@PathVariable Long id) {
        return service.disconnect(id);
    }
}
