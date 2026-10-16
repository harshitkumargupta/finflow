package com.finflow.service;

import com.finflow.dto.FinancialReportResponse;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDate;

@Service
public class FinancialReportService {
    private final AnalyticsService analyticsService;
    private final BudgetService budgetService;
    private final BillService billService;
    private final SavingsGoalService goalService;

    public FinancialReportService(AnalyticsService analyticsService,
                                 BudgetService budgetService,
                                 BillService billService,
                                 SavingsGoalService goalService) {
        this.analyticsService = analyticsService;
        this.budgetService = budgetService;
        this.billService = billService;
        this.goalService = goalService;
    }

    public FinancialReportResponse getReport() {
        LocalDate today = LocalDate.now();
        var upcomingBills = billService.upcoming();
        BigDecimal monthlyObligations = upcomingBills.stream()
                .filter(bill -> !bill.nextDueDate().isBefore(today.withDayOfMonth(1))
                        && !bill.nextDueDate().isAfter(today.withDayOfMonth(today.lengthOfMonth())))
                .map(bill -> bill.amount())
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        return new FinancialReportResponse(
                analyticsService.getSummary(),
                budgetService.list(),
                upcomingBills,
                goalService.list(),
                monthlyObligations);
    }
}
