package com.finflow;

import com.finflow.dto.SafeToSpendBreakdown;
import com.finflow.dto.SafeToSpendChangeDriver;
import com.finflow.dto.SafeToSpendResponse;
import com.finflow.entity.IncomeSchedule;
import com.finflow.service.IncomeScheduleService;
import com.finflow.service.SafeToSpendChangeService;
import com.finflow.service.SafeToSpendService;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class SafeToSpendChangeTest {

    private static final LocalDate TODAY = LocalDate.of(2026, 10, 2);

    @Test
    void driversExplainTheWholeChange() {
        List<SafeToSpendBreakdown> before = List.of(
                new SafeToSpendBreakdown("UPCOMING_BILL", "Rent", new BigDecimal("15000.00")),
                new SafeToSpendBreakdown("BUDGET_RESERVE", "FOOD", new BigDecimal("3000.00")));
        SafeToSpendResponse now = new SafeToSpendResponse(new BigDecimal("40000.00"), new BigDecimal("29500.00"),
                new BigDecimal("983.33"), TODAY.plusDays(30), List.of(
                new SafeToSpendBreakdown("BUDGET_RESERVE", "FOOD", new BigDecimal("2500.00")),
                new SafeToSpendBreakdown("AUTOPAY", "Netflix", new BigDecimal("649.00")),
                new SafeToSpendBreakdown(SafeToSpendService.EXPECTED_INCOME, "Salary", new BigDecimal("50000.00")),
                new SafeToSpendBreakdown("UPCOMING_BILL", "Electricity", new BigDecimal("57351.00"))));

        var result = SafeToSpendChangeService.explain(TODAY.minusDays(1), new BigDecimal("25000.00"),
                new BigDecimal("7000.00"), before, now);

        assertTrue(result.baselineAvailable());
        assertEquals(new BigDecimal("22500.00"), result.change());
        BigDecimal explained = result.drivers().stream().map(SafeToSpendChangeDriver::impact).reduce(BigDecimal.ZERO, BigDecimal::add);
        assertEquals(0, result.change().compareTo(explained));
        assertEquals("Electricity", result.drivers().get(0).description());
        assertEquals(new BigDecimal("-57351.00"), result.drivers().get(0).impact());
        assertTrue(result.summary().startsWith("Safe-to-Spend is up ₹22500.00"));
        SafeToSpendChangeDriver rent = driver(result.drivers(), "Rent");
        assertEquals(new BigDecimal("15000.00"), rent.impact());
        assertTrue(rent.explanation().contains("no longer counted"));
        assertEquals(new BigDecimal("50000.00"), driver(result.drivers(), "Salary").impact());
        assertEquals(new BigDecimal("500.00"), driver(result.drivers(), "FOOD").impact());
        assertEquals(new BigDecimal("15000.00"), driver(result.drivers(), "Account balance").impact());
    }

    @Test
    void unchangedInputsProduceNoDrivers() {
        List<SafeToSpendBreakdown> items = List.of(new SafeToSpendBreakdown("UPCOMING_BILL", "Rent", new BigDecimal("100.00")));
        SafeToSpendResponse now = new SafeToSpendResponse(new BigDecimal("500.00"), new BigDecimal("400.00"),
                new BigDecimal("13.33"), TODAY.plusDays(30), items);
        var result = SafeToSpendChangeService.explain(TODAY.minusDays(1), new BigDecimal("500.00"), new BigDecimal("400.00"), items, now);
        assertTrue(result.drivers().isEmpty());
        assertEquals(0, result.change().signum());
    }

    @Test
    void incomeOccurrencesAreCountedOnlyAfterTodayAndWithinHorizon() {
        LocalDate horizon = TODAY.plusDays(30);
        assertEquals(List.of(TODAY.plusDays(7), TODAY.plusDays(14), TODAY.plusDays(21), TODAY.plusDays(28)),
                IncomeScheduleService.occurrencesWithin(schedule("WEEKLY", TODAY), TODAY, horizon));
        assertEquals(List.of(LocalDate.of(2026, 10, 25)),
                IncomeScheduleService.occurrencesWithin(schedule("MONTHLY", LocalDate.of(2023, 1, 25)), TODAY, horizon));
        assertTrue(IncomeScheduleService.occurrencesWithin(schedule("ONE_TIME", TODAY.minusDays(1)), TODAY, horizon).isEmpty());
        assertTrue(IncomeScheduleService.occurrencesWithin(schedule("ONE_TIME", TODAY.plusDays(31)), TODAY, horizon).isEmpty());
        IncomeSchedule inactive = schedule("WEEKLY", TODAY.plusDays(1));
        inactive.setActive(false);
        assertTrue(IncomeScheduleService.occurrencesWithin(inactive, TODAY, horizon).isEmpty());
    }

    @Test
    void expectedIncomeRaisesSafeToSpend() {
        var result = new SafeToSpendService(null, null, null, null, null, null).calculate(new BigDecimal("1000.00"),
                List.of(), List.of(), List.of(), List.of(schedule("BIWEEKLY", TODAY.plusDays(3))), TODAY);
        assertEquals(new BigDecimal("2000.00"), result.safeToSpend());
        assertEquals(SafeToSpendService.EXPECTED_INCOME, result.breakdown().get(0).type());
        assertEquals(new BigDecimal("1000.00"), result.breakdown().get(0).amount());
    }

    private SafeToSpendChangeDriver driver(List<SafeToSpendChangeDriver> drivers, String description) {
        return drivers.stream().filter(d -> d.description().equals(description)).findFirst().orElseThrow();
    }

    private IncomeSchedule schedule(String frequency, LocalDate next) {
        IncomeSchedule schedule = new IncomeSchedule();
        schedule.setSourceName("Salary");
        schedule.setAmount(new BigDecimal("500.00"));
        schedule.setFrequency(frequency);
        schedule.setNextExpectedDate(next);
        return schedule;
    }
}
