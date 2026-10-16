package com.finflow.repository;

import com.finflow.entity.FamilyWalletTransaction;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public interface FamilyWalletTransactionRepository extends JpaRepository<FamilyWalletTransaction, Long> {
    List<FamilyWalletTransaction> findByFamilyMemberIdOrderByCreatedAtDesc(Long familyMemberId);
    Optional<FamilyWalletTransaction> findByFamilyMemberIdAndIdempotencyKey(
            Long familyMemberId, String idempotencyKey);

    @Query("""
            SELECT COALESCE(SUM(t.amount), 0) FROM FamilyWalletTransaction t
            WHERE t.familyMember.id = :memberId AND t.type = 'SPEND'
              AND t.status = 'SUCCESS' AND t.createdAt >= :start AND t.createdAt < :end
            """)
    BigDecimal sumMonthlySpends(@Param("memberId") Long memberId,
                                @Param("start") LocalDateTime start,
                                @Param("end") LocalDateTime end);

    @Query("""
            SELECT COALESCE(SUM(t.amount), 0) FROM FamilyWalletTransaction t
            WHERE t.familyMember.id = :memberId AND t.type = 'SPEND'
              AND t.status = 'SUCCESS' AND t.category = :category
              AND t.createdAt >= :start AND t.createdAt < :end
            """)
    BigDecimal sumMonthlySpendsByCategory(@Param("memberId") Long memberId,
                                          @Param("category") String category,
                                          @Param("start") LocalDateTime start,
                                          @Param("end") LocalDateTime end);
}
