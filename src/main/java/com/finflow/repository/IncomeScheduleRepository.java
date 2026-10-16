package com.finflow.repository;

import com.finflow.entity.IncomeSchedule;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface IncomeScheduleRepository extends JpaRepository<IncomeSchedule, Long> {
    List<IncomeSchedule> findByUserIdOrderByNextExpectedDateAscIdAsc(Long userId);
    List<IncomeSchedule> findByUserIdAndActiveTrueOrderByNextExpectedDateAsc(Long userId);
    Optional<IncomeSchedule> findByIdAndUserId(Long id, Long userId);
}
