package com.finflow.repository;

import com.finflow.entity.Budget;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface BudgetRepository extends JpaRepository<Budget, Long> {
    List<Budget> findByUserIdOrderByStartDateDesc(Long userId);
    Optional<Budget> findByIdAndUserId(Long id, Long userId);
}
