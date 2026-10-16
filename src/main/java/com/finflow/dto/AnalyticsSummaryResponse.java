package com.finflow.dto;

import java.math.BigDecimal;

public class AnalyticsSummaryResponse {

    private final BigDecimal currentBalance;
    private final BigDecimal totalIncome;
    private final BigDecimal totalExpense;
    private final BigDecimal totalDeposits;
    private final BigDecimal totalWithdrawals;
    private final BigDecimal totalTransfersIn;
    private final BigDecimal totalTransfersOut;
    private final long totalTransactions;

    public AnalyticsSummaryResponse(
            BigDecimal currentBalance,
            BigDecimal totalIncome,
            BigDecimal totalExpense,
            BigDecimal totalDeposits,
            BigDecimal totalWithdrawals,
            BigDecimal totalTransfersIn,
            BigDecimal totalTransfersOut,
            long totalTransactions) {
        this.currentBalance = currentBalance;
        this.totalIncome = totalIncome;
        this.totalExpense = totalExpense;
        this.totalDeposits = totalDeposits;
        this.totalWithdrawals = totalWithdrawals;
        this.totalTransfersIn = totalTransfersIn;
        this.totalTransfersOut = totalTransfersOut;
        this.totalTransactions = totalTransactions;
    }

    public BigDecimal getCurrentBalance() {
        return currentBalance;
    }

    public BigDecimal getTotalIncome() {
        return totalIncome;
    }

    public BigDecimal getTotalExpense() {
        return totalExpense;
    }

    public BigDecimal getTotalDeposits() {
        return totalDeposits;
    }

    public BigDecimal getTotalWithdrawals() {
        return totalWithdrawals;
    }

    public BigDecimal getTotalTransfersIn() {
        return totalTransfersIn;
    }

    public BigDecimal getTotalTransfersOut() {
        return totalTransfersOut;
    }

    public long getTotalTransactions() {
        return totalTransactions;
    }
}
