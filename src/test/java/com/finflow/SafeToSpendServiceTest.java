package com.finflow;

import com.finflow.dto.BillResponse;
import com.finflow.dto.BudgetResponse;
import com.finflow.entity.AutopayAuthorization;
import com.finflow.service.SafeToSpendService;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

class SafeToSpendServiceTest {
    private final LocalDate today = LocalDate.of(2026, 10, 1);

    @Test
    void positiveResultIncludesBillAndBudgetReserve() {
        var result = calculate(new BigDecimal("1000"), List.of(bill("Rent", 200)), List.of(), List.of(budget(300, 100)));
        assertEquals(new BigDecimal("600.00"), result.safeToSpend().setScale(2));
        assertEquals(2, result.breakdown().size());
    }

    @Test
    void negativeResultIsPreserved() {
        var result = calculate(new BigDecimal("1000"), List.of(bill("Large bill", 1500)), List.of(), List.of());
        assertEquals(new BigDecimal("-500.00"), result.safeToSpend().setScale(2));
    }

    @Test
    void pausedAutopayIsExcludedAndActiveAutopayIsIncluded() {
        var result = calculate(new BigDecimal("1000"), List.of(),
                List.of(autopay("Paused", "PAUSED", 400), autopay("Active", "ACTIVE", 100)), List.of());
        assertEquals(new BigDecimal("900.00"), result.safeToSpend().setScale(2));
    }

    @Test
    void noBillsLeavesBalanceAvailable() {
        assertEquals(new BigDecimal("1000.00"), calculate(new BigDecimal("1000"), List.of(), List.of(), List.of()).safeToSpend().setScale(2));
    }

    @Test
    void goalContributionAutopayIsIncluded() {
        var result = calculate(new BigDecimal("1000"), List.of(), List.of(autopay("Goal", "ACTIVE", 125)), List.of());
        assertEquals(new BigDecimal("875.00"), result.safeToSpend().setScale(2));
    }

    private com.finflow.dto.SafeToSpendResponse calculate(BigDecimal balance, List<BillResponse> bills,
                                                           List<AutopayAuthorization> autopays, List<BudgetResponse> budgets) {
        return new SafeToSpendService(null, null, null, null, null, null).calculate(balance, bills, autopays, budgets, today);
    }

    private BillResponse bill(String name, int amount) {
        return new BillResponse(1L, name, BigDecimal.valueOf(amount), "BILLS", "ONCE", today.plusDays(2), null,
                1L, null, true, true, 2);
    }

    private AutopayAuthorization autopay(String name, String status, int amount) {
        AutopayAuthorization result = new AutopayAuthorization();
        result.setMerchantName(name); result.setStatus(status); result.setAmount(BigDecimal.valueOf(amount));
        result.setNextPaymentDate(today.plusDays(1));
        return result;
    }

    private BudgetResponse budget(int limit, int spent) {
        return new BudgetResponse(1L, "FOOD", BigDecimal.valueOf(limit), BigDecimal.valueOf(spent),
                BigDecimal.valueOf(limit - spent), BigDecimal.valueOf(spent * 100.0 / limit), false,
                today.minusDays(2), today.plusDays(28), "ACTIVE", null, "OK",
                BigDecimal.valueOf(spent * 100.0 / limit));
    }
}
