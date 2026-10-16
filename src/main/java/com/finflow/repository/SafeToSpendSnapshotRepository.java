package com.finflow.repository;

import com.finflow.entity.SafeToSpendSnapshot;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDate;
import java.util.Optional;

public interface SafeToSpendSnapshotRepository extends JpaRepository<SafeToSpendSnapshot, Long> {
    Optional<SafeToSpendSnapshot> findByUserIdAndSnapshotDate(Long userId, LocalDate snapshotDate);
    Optional<SafeToSpendSnapshot> findFirstByUserIdAndSnapshotDateBeforeOrderBySnapshotDateDesc(Long userId, LocalDate snapshotDate);
}
