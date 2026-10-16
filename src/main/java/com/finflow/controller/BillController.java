package com.finflow.controller;

import org.springframework.web.bind.annotation.RequestParam;
import com.finflow.dto.BillRequest;
import com.finflow.dto.BillResponse;
import com.finflow.service.BillService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/bills")
public class BillController {
    private final BillService billService;

    public BillController(BillService billService) { this.billService = billService; }

    @PostMapping
    public ResponseEntity<BillResponse> create(@Valid @RequestBody BillRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(billService.create(request));
    }

    @GetMapping
    public Object list(@RequestParam(required = false) Integer page, @RequestParam(required = false) Integer size) {
        return Paging.requested(page, size) ? billService.list(Paging.of(page, size)) : billService.list();
    }

    @GetMapping("/upcoming")
    public List<BillResponse> upcoming() { return billService.upcoming(); }

    @GetMapping("/summary")
    public Map<String, Object> summary() {
        List<BillResponse> due = billService.upcoming();
        java.time.LocalDate today = java.time.LocalDate.now();
        List<BillResponse> thisMonth = due.stream().filter(bill ->
                !bill.nextDueDate().isBefore(today.withDayOfMonth(1))
                        && !bill.nextDueDate().isAfter(today.withDayOfMonth(today.lengthOfMonth())))
                .toList();
        BigDecimal total = thisMonth.stream()
                .map(BillResponse::amount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        long dueSoon = due.stream().filter(bill -> bill.daysUntilDue() <= 7).count();
        return Map.of("upcomingThisMonth", thisMonth.size(), "dueSoon", dueSoon,
                "totalUpcomingObligations", total);
    }

    @GetMapping("/{id}")
    public BillResponse get(@PathVariable Long id) { return billService.get(id); }

    @PutMapping("/{id}")
    public BillResponse update(@PathVariable Long id, @Valid @RequestBody BillRequest request) {
        return billService.update(id, request);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        billService.delete(id);
        return ResponseEntity.noContent().build();
    }
}
