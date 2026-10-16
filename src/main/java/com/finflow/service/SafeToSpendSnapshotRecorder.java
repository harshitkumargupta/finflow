package com.finflow.service;

import com.finflow.dto.SafeToSpendResponse;
import com.finflow.entity.SafeToSpendSnapshot;
import com.finflow.repository.SafeToSpendSnapshotRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import tools.jackson.databind.json.JsonMapper;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Service
public class SafeToSpendSnapshotRecorder {

    static final JsonMapper JSON = JsonMapper.builder().build();

    private final SafeToSpendSnapshotRepository repository;

    public SafeToSpendSnapshotRecorder(SafeToSpendSnapshotRepository repository) {
        this.repository = repository;
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void recordIfAbsent(Long userId, LocalDate date, SafeToSpendResponse response) {
        if (repository.findByUserIdAndSnapshotDate(userId, date).isPresent()) return;
        repository.saveAndFlush(new SafeToSpendSnapshot(userId, date, response.currentBalance(), response.safeToSpend(),
                JSON.writeValueAsString(response.breakdown()), LocalDateTime.now()));
    }
}
