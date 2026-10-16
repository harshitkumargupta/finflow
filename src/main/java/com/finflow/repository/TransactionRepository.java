package com.finflow.repository;

import com.finflow.entity.Transaction;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface TransactionRepository
        extends JpaRepository<Transaction, Long> {

    Optional<Transaction> findByIdempotencyKey(
            String idempotencyKey
    );

    List<Transaction> findByAccountIdOrderByCreatedAtDesc(
            Long accountId
    );
    Page<Transaction> findByAccountIdOrderByCreatedAtDesc(Long accountId, Pageable pageable);

    List<Transaction> findByAccountId(Long accountId);

    List<Transaction> findByAccountIdAndStatus(
            Long accountId,
            String status
    );

    @Query("""
            SELECT COALESCE(SUM(t.amount), 0)
            FROM Transaction t
            WHERE t.account.user.id = :userId
              AND t.status = 'SUCCESS'
              AND t.type IN ('WITHDRAWAL', 'TRANSFER_OUT')
              AND t.createdAt >= :start
              AND t.createdAt <= :end
              AND (:category = 'OVERALL' OR t.category = :category)
            """)
    BigDecimal sumExpensesForBudget(
            @Param("userId") Long userId,
            @Param("category") String category,
            @Param("start") LocalDateTime start,
            @Param("end") LocalDateTime end);

    @Query("""
            SELECT t
            FROM Transaction t JOIN FETCH t.account a
            WHERE a.user.id = :userId
              AND t.status = 'SUCCESS'
            ORDER BY t.createdAt DESC
            """)
    List<Transaction> findSuccessfulByUserIdOrderByCreatedAtDesc(
            @Param("userId") Long userId);

    @Query(value = """
            SELECT
                COALESCE(SUM(CASE WHEN t.type = 'DEPOSIT' THEN t.amount ELSE 0 END), 0),
                COALESCE(SUM(CASE WHEN t.type = 'WITHDRAWAL' THEN t.amount ELSE 0 END), 0),
                COALESCE(SUM(CASE WHEN t.type = 'TRANSFER_IN' THEN t.amount ELSE 0 END), 0),
                COALESCE(SUM(CASE WHEN t.type = 'TRANSFER_OUT' THEN t.amount ELSE 0 END), 0),
                COUNT(t.id)
            FROM transactions t
            JOIN accounts a ON a.id = t.account_id
            WHERE a.user_id = :userId
              AND t.status = 'SUCCESS'
            """, nativeQuery = true)
    List<Object[]> getSuccessfulTotalsByUserId(@Param("userId") Long userId);

    @Query(value = """
            SELECT t.category, SUM(t.amount), COUNT(t.id)
            FROM transactions t
            JOIN accounts a ON a.id = t.account_id
            WHERE a.user_id = :userId
              AND t.status = 'SUCCESS'
              AND t.type IN ('WITHDRAWAL', 'TRANSFER_OUT')
              AND t.category IS NOT NULL
              AND TRIM(t.category) <> ''
            GROUP BY t.category
            """, nativeQuery = true)
    List<Object[]> getSuccessfulExpenseTotalsByCategory(
            @Param("userId") Long userId
    );

    @Query(value = """
            SELECT
                TO_CHAR(t.created_at, 'YYYY-MM') AS month,
                COALESCE(SUM(CASE WHEN t.type = 'DEPOSIT' THEN t.amount ELSE 0 END), 0) AS deposits,
                COALESCE(SUM(CASE WHEN t.type = 'WITHDRAWAL' THEN t.amount ELSE 0 END), 0) AS withdrawals,
                COALESCE(SUM(CASE WHEN t.type = 'TRANSFER_IN' THEN t.amount ELSE 0 END), 0) AS transfers_in,
                COALESCE(SUM(CASE WHEN t.type = 'TRANSFER_OUT' THEN t.amount ELSE 0 END), 0) AS transfers_out,
                COUNT(t.id) AS transaction_count
            FROM transactions t
            JOIN accounts a ON a.id = t.account_id
            WHERE a.user_id = :userId
              AND t.status = 'SUCCESS'
            GROUP BY TO_CHAR(t.created_at, 'YYYY-MM')
            ORDER BY month ASC
            """, nativeQuery = true)
    List<Object[]> getSuccessfulMonthlyTotalsByUserId(
            @Param("userId") Long userId
    );
}
