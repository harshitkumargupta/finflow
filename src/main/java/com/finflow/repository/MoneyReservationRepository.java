package com.finflow.repository;

import com.finflow.entity.MoneyReservation;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

public interface MoneyReservationRepository extends JpaRepository<MoneyReservation, Long> {
    @Query("SELECT COALESCE(SUM(r.amount), 0) FROM MoneyReservation r WHERE r.account.id = :accountId AND r.status = 'ACTIVE'")
    BigDecimal sumActiveByAccountId(@Param("accountId") Long accountId);

    @Query("SELECT COALESCE(SUM(r.amount), 0) FROM MoneyReservation r WHERE r.user.id = :userId AND r.status = 'ACTIVE'")
    BigDecimal sumActiveByUserId(@Param("userId") Long userId);

    List<MoneyReservation> findByUserIdAndStatusOrderByCreatedAtDesc(Long userId, String status);
    List<MoneyReservation> findByFamilyMemberIdAndAccountIdAndStatusOrderByCreatedAtAsc(
            Long familyMemberId, Long accountId, String status);
    List<MoneyReservation> findByFamilyMemberIdAndStatusOrderByCreatedAtDesc(
            Long familyMemberId, String status);
    @Query("SELECT COALESCE(SUM(r.amount), 0) FROM MoneyReservation r WHERE r.familyMember.id = :memberId AND r.status = 'ACTIVE'")
    BigDecimal sumActiveByFamilyMemberId(@Param("memberId") Long memberId);
    Optional<MoneyReservation> findByIdAndUserId(Long id, Long userId);
    boolean existsByUserIdAndSourceKey(Long userId, String sourceKey);
}
