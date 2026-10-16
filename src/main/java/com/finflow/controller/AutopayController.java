package com.finflow.controller;

import org.springframework.web.bind.annotation.RequestParam;
import com.finflow.dto.AutopayAuthorizationResponse;
import com.finflow.dto.AutopayEditRequest;
import com.finflow.dto.AutopayExecutionResponse;
import com.finflow.dto.AutopayRequest;
import com.finflow.dto.AutopayApprovalRequest;
import com.finflow.service.AutopayAuthorizationService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/autopay")
public class AutopayController {
    private final AutopayAuthorizationService service;

    public AutopayController(AutopayAuthorizationService service) {
        this.service = service;
    }

    @PostMapping
    public ResponseEntity<AutopayAuthorizationResponse> create(
            @Valid @RequestBody AutopayRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(service.create(request));
    }

    @PostMapping("/simulate-incoming")
    public ResponseEntity<AutopayAuthorizationResponse> simulateIncoming(
            @Valid @RequestBody com.finflow.dto.SimulateMandateRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(service.simulateIncomingMandate(request));
    }

    @PutMapping("/{id}/approve")
    public ResponseEntity<AutopayAuthorizationResponse> approve(
            @PathVariable Long id,
            @Valid @RequestBody AutopayApprovalRequest request) {
        return ResponseEntity.ok(service.approveMandate(id, request));
    }

    @PutMapping("/{id}/reject")
    public ResponseEntity<AutopayAuthorizationResponse> reject(
            @PathVariable Long id) {
        return ResponseEntity.ok(service.rejectMandate(id));
    }

    @GetMapping
    public List<AutopayAuthorizationResponse> list() {
        return service.list();
    }

    @GetMapping("/upcoming")
    public List<AutopayAuthorizationResponse> getUpcoming() {
        return service.getUpcomingPayments();
    }

    @GetMapping("/history")
    public Object getAllHistory(@RequestParam(required = false) Integer page, @RequestParam(required = false) Integer size) {
        if (Paging.requested(page, size)) return service.getExecutionHistory(null, Paging.of(page, size));
        return service.getExecutionHistory(null);
    }

    @GetMapping("/{id}")
    public AutopayAuthorizationResponse get(@PathVariable Long id) {
        return service.get(id);
    }

    @GetMapping("/{id}/history")
    public Object getHistory(@PathVariable Long id, @RequestParam(required = false) Integer page, @RequestParam(required = false) Integer size) {
        if (Paging.requested(page, size)) return service.getExecutionHistory(id, Paging.of(page, size));
        return service.getExecutionHistory(id);
    }

    @PutMapping("/{id}")
    public AutopayAuthorizationResponse edit(
            @PathVariable Long id,
            @Valid @RequestBody AutopayEditRequest request) {
        return service.edit(id, request);
    }

    @PutMapping("/{id}/pause")
    public AutopayAuthorizationResponse pause(@PathVariable Long id) {
        return service.pause(id);
    }

    @PutMapping("/{id}/resume")
    public AutopayAuthorizationResponse resume(@PathVariable Long id) {
        return service.resume(id);
    }

    @DeleteMapping("/{id}")
    public AutopayAuthorizationResponse cancel(@PathVariable Long id) {
        return service.cancel(id);
    }

    @PostMapping("/{id}/execute")
    public AutopayExecutionResponse execute(@PathVariable Long id) {
        return service.executeSimulatedPayment(id);
    }
}
