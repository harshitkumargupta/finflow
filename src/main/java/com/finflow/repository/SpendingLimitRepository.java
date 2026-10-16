package com.finflow.repository;

import com.finflow.entity.SpendingLimit;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface SpendingLimitRepository extends JpaRepository<SpendingLimit, Long> {
    List<SpendingLimit> findByUserIdOrderByScopeTypeAscCategoryAsc(Long userId);
    Optional<SpendingLimit> findByIdAndUserId(Long id, Long userId);
    Optional<SpendingLimit> findByUserIdAndScopeTypeAndCategory(Long userId, String scopeType, String category);
}
