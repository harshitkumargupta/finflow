package com.finflow;

import com.finflow.dto.MonthlyReportResponse;
import com.finflow.entity.Transaction;
import com.finflow.entity.User;
import com.finflow.insight.InsightNarrationProvider;
import com.finflow.repository.AutopayAuthorizationRepository;
import com.finflow.repository.BillRepository;
import com.finflow.repository.SavingsGoalRepository;
import com.finflow.repository.TransactionRepository;
import com.finflow.service.MonthlyReportService;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.YearMonth;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class MonthlyReportServiceTest {

    private final TransactionRepository txRepo = mock(TransactionRepository.class);
    private final BillRepository billRepo = mock(BillRepository.class);
    private final AutopayAuthorizationRepository autopayRepo = mock(AutopayAuthorizationRepository.class);
    private final SavingsGoalRepository goalRepo = mock(SavingsGoalRepository.class);
    private final InsightNarrationProvider provider = mock(InsightNarrationProvider.class);

    private MonthlyReportService service() {
        when(billRepo.findByUserIdAndActiveTrueAndNextDueDateBetweenOrderByNextDueDateAsc(anyLong(), any(), any())).thenReturn(List.of());
        when(autopayRepo.findByUserIdOrderByNextPaymentDateAsc(anyLong())).thenReturn(List.of());
        when(goalRepo.findByUserIdOrderByCreatedAtDesc(anyLong())).thenReturn(List.of());
        when(provider.available()).thenReturn(false);
        return new MonthlyReportService(txRepo, billRepo, autopayRepo, goalRepo, provider);
    }

    private Transaction tx(String type, String amount, String category, LocalDateTime createdAt) {
        Transaction transaction = mock(Transaction.class);
        when(transaction.getType()).thenReturn(type);
        when(transaction.getAmount()).thenReturn(new BigDecimal(amount));
        when(transaction.getCategory()).thenReturn(category);
        when(transaction.getCreatedAt()).thenReturn(createdAt);
        return transaction;
    }

    private User user() {
        User user = mock(User.class);
        when(user.getId()).thenReturn(1L);
        return user;
    }

    @Test
    void summarisesOnlyTransactionsInTheRequestedMonth() {
        Transaction deposit = tx("DEPOSIT", "1000.00", "DEPOSIT", LocalDateTime.of(2026, 9, 10, 10, 0));
        Transaction food = tx("WITHDRAWAL", "200.00", "FOOD", LocalDateTime.of(2026, 9, 5, 10, 0));
        Transaction lastMonth = tx("WITHDRAWAL", "50.00", "TRANSPORT", LocalDateTime.of(2026, 8, 30, 10, 0));
        List<Transaction> all = List.of(deposit, food, lastMonth);
        when(txRepo.findSuccessfulByUserIdOrderByCreatedAtDesc(eq(1L))).thenReturn(all);

        MonthlyReportResponse report = service().build(user(), YearMonth.of(2026, 9));

        assertTrue(report.hasActivity());
        assertEquals(0, new BigDecimal("1000.00").compareTo(report.totalIn()));
        assertEquals(0, new BigDecimal("200.00").compareTo(report.totalOut()));
        assertEquals(0, new BigDecimal("800.00").compareTo(report.net()));
        assertEquals(2, report.transactionCount());
        assertEquals("FOOD", report.topCategories().get(0).category());
        assertEquals("NONE", report.narrationSource());
        assertFalse(report.aiGenerated());
    }

    @Test
    void reportsHonestEmptyStateForMonthWithoutActivity() {
        Transaction older = tx("WITHDRAWAL", "50.00", "FOOD", LocalDateTime.of(2026, 7, 30, 10, 0));
        List<Transaction> all = List.of(older);
        when(txRepo.findSuccessfulByUserIdOrderByCreatedAtDesc(eq(1L))).thenReturn(all);

        MonthlyReportResponse report = service().build(user(), YearMonth.of(2026, 9));

        assertFalse(report.hasActivity());
        assertEquals(0, report.transactionCount());
        assertTrue(report.message().toLowerCase().contains("no transactions"));
    }
}
