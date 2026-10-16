package com.finflow.repository;

import com.finflow.entity.Family;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface FamilyRepository extends JpaRepository<Family, Long> {
    Optional<Family> findByParentId(Long parentId);
}
