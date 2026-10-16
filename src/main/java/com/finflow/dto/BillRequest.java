package com.finflow.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;
import java.time.LocalDate;

public class BillRequest {
    @NotBlank @Size(max = 120)
    private String name;
    @NotNull @DecimalMin("0.01")
    private BigDecimal amount;
    @NotBlank @Pattern(regexp = "FOOD|GROCERIES|TRANSPORT|SHOPPING|BILLS|ENTERTAINMENT|HEALTH|EDUCATION|OTHER")
    private String category;
    @NotBlank @Pattern(regexp = "ONE_TIME|WEEKLY|MONTHLY|QUARTERLY|YEARLY")
    private String frequency;
    @NotNull
    private LocalDate nextDueDate;
    @Size(max = 255)
    private String description;
    private Long accountId;
    private boolean reminderEnabled = true;
    private boolean active = true;

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public BigDecimal getAmount() { return amount; }
    public void setAmount(BigDecimal amount) { this.amount = amount; }
    public String getCategory() { return category; }
    public void setCategory(String category) { this.category = category; }
    public String getFrequency() { return frequency; }
    public void setFrequency(String frequency) { this.frequency = frequency; }
    public LocalDate getNextDueDate() { return nextDueDate; }
    public void setNextDueDate(LocalDate nextDueDate) { this.nextDueDate = nextDueDate; }
    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }
    public Long getAccountId() { return accountId; }
    public void setAccountId(Long accountId) { this.accountId = accountId; }
    public boolean isReminderEnabled() { return reminderEnabled; }
    public void setReminderEnabled(boolean reminderEnabled) { this.reminderEnabled = reminderEnabled; }
    public boolean isActive() { return active; }
    public void setActive(boolean active) { this.active = active; }
}
