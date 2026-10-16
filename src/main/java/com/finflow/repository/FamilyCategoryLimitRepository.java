package com.finflow.repository;

import com.finflow.entity.FamilyCategoryLimit;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface FamilyCategoryLimitRepository extends JpaRepository<FamilyCategoryLimit, Long> {
    List<FamilyCategoryLimit> findByFamilyMemberIdOrderByCategoryAsc(Long familyMemberId);
    Optional<FamilyCategoryLimit> findByIdAndFamilyMemberId(Long id, Long familyMemberId);
    Optional<FamilyCategoryLimit> findByFamilyMemberIdAndCategory(Long familyMemberId, String category);
}
