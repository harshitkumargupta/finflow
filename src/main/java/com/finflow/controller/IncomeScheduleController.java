package com.finflow.controller;

import com.finflow.dto.IncomeScheduleRequest;
import com.finflow.dto.IncomeScheduleResponse;
import com.finflow.service.IncomeScheduleService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/income-schedules")
public class IncomeScheduleController {

    private final IncomeScheduleService service;

    public IncomeScheduleController(IncomeScheduleService service) {
        this.service = service;
    }

    @GetMapping
    public List<IncomeScheduleResponse> list() {
        return service.list();
    }

    @PostMapping
    public ResponseEntity<IncomeScheduleResponse> create(@Valid @RequestBody IncomeScheduleRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(service.create(request));
    }

    @PutMapping("/{id}")
    public IncomeScheduleResponse update(@PathVariable Long id, @Valid @RequestBody IncomeScheduleRequest request) {
        return service.update(id, request);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        service.delete(id);
        return ResponseEntity.noContent().build();
    }
}
