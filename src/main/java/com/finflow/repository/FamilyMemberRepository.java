package com.finflow.repository;

import com.finflow.entity.FamilyMember;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface FamilyMemberRepository extends JpaRepository<FamilyMember, Long> {
    List<FamilyMember> findByParentIdOrderByCreatedAtDesc(Long parentId);
    List<FamilyMember> findByParentIdAndStatusNotOrderByCreatedAtDesc(Long parentId, String status);
    Optional<FamilyMember> findByIdAndParentId(Long id, Long parentId);
}
