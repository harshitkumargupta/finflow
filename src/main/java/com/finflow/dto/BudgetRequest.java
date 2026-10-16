package com.finflow.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;

import java.math.BigDecimal;
import java.time.LocalDate;

public class BudgetRequest {
    @NotBlank
    @Pattern(regexp = "OVERALL|FOOD|GROCERIES|TRANSPORT|SHOPPING|BILLS|ENTERTAINMENT|HEALTH|EDUCATION|OTHER")
    private String category;
    @NotNull @DecimalMin("0.01")
    private BigDecimal budgetAmount;
    @NotNull
    private LocalDate startDate;
    @NotNull
    private LocalDate endDate;

    public String getCategory() { return category; }
    public void setCategory(String category) { this.category = category; }
    public BigDecimal getBudgetAmount() { return budgetAmount; }
    public void setBudgetAmount(BigDecimal budgetAmount) { this.budgetAmount = budgetAmount; }
    public LocalDate getStartDate() { return startDate; }
    public void setStartDate(LocalDate startDate) { this.startDate = startDate; }
    public LocalDate getEndDate() { return endDate; }
    public void setEndDate(LocalDate endDate) { this.endDate = endDate; }
}
