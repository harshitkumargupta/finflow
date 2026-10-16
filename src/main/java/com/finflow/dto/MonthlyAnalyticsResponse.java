package com.finflow.dto;

import java.math.BigDecimal;

public class MonthlyAnalyticsResponse {

    private final String month;
    private final BigDecimal income;
    private final BigDecimal expense;
    private final BigDecimal net;
    private final BigDecimal deposits;
    private final BigDecimal withdrawals;
    private final BigDecimal transfersIn;
    private final BigDecimal transfersOut;
    private final long transactionCount;

    public MonthlyAnalyticsResponse(
            String month,
            BigDecimal income,
            BigDecimal expense,
            BigDecimal net,
            BigDecimal deposits,
            BigDecimal withdrawals,
            BigDecimal transfersIn,
            BigDecimal transfersOut,
            long transactionCount) {
        this.month = month;
        this.income = income;
        this.expense = expense;
        this.net = net;
        this.deposits = deposits;
        this.withdrawals = withdrawals;
        this.transfersIn = transfersIn;
        this.transfersOut = transfersOut;
        this.transactionCount = transactionCount;
    }

    public MonthlyAnalyticsResponse(
            String month,
            BigDecimal income,
            BigDecimal expense,
            BigDecimal net) {
        this(
                month,
                income,
                expense,
                net,
                BigDecimal.ZERO,
                BigDecimal.ZERO,
                BigDecimal.ZERO,
                BigDecimal.ZERO,
                0
        );
    }

    public String getMonth() {
        return month;
    }

    public BigDecimal getIncome() {
        return income;
    }

    public BigDecimal getExpense() {
        return expense;
    }

    public BigDecimal getNet() {
        return net;
    }

    public BigDecimal getDeposits() {
        return deposits;
    }

    public BigDecimal getWithdrawals() {
        return withdrawals;
    }

    public BigDecimal getTransfersIn() {
        return transfersIn;
    }

    public BigDecimal getTransfersOut() {
        return transfersOut;
    }

    public long getTransactionCount() {
        return transactionCount;
    }
}
