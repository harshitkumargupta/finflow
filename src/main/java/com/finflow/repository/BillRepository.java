package com.finflow.repository;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import com.finflow.entity.Bill;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

public interface BillRepository extends JpaRepository<Bill, Long> {
    List<Bill> findByUserIdOrderByNextDueDateAsc(Long userId);
    List<Bill> findByUserIdAndActiveTrueAndNextDueDateBetweenOrderByNextDueDateAsc(
            Long userId, LocalDate start, LocalDate end);
    List<Bill> findByUserIdAndActiveTrueAndNextDueDateLessThanEqualOrderByNextDueDateAsc(
            Long userId, LocalDate end);
    Optional<Bill> findByIdAndUserId(Long id, Long userId);

    Page<Bill> findByUserIdOrderByNextDueDateAscIdAsc(Long userId, Pageable pageable);
}
