package com.finflow.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;

public class MoneyRequest {

    @Pattern(
            regexp = "FOOD|GROCERIES|SHOPPING|CLOTHING|MEDICAL|TRANSPORT|BILLS|RENT|EDUCATION|ENTERTAINMENT|INVESTMENT|TRANSFER|DEPOSIT|OTHER",
            message = "Category is not supported"
    )
    private String category;

    @Size(max = 255, message = "Description must not exceed 255 characters")
    private String description;

    @NotNull(message = "Amount is required")
    @DecimalMin(
            value = "0.01",
            message = "Amount must be greater than zero"
    )
    @Digits(integer = 17, fraction = 2, message = "Amount can have at most two decimal places")
    private BigDecimal amount;

    @Pattern(regexp = "^[0-9]{6}$", message = "Transaction PIN must be 6 digits")
    private String pin;

    public MoneyRequest() {
    }

    public String getCategory() {
        return category;
    }

    public void setCategory(String category) {
        this.category = category;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public BigDecimal getAmount() {
        return amount;
    }

    public void setAmount(BigDecimal amount) {
        this.amount = amount;
    }

    public String getPin() { return pin; }
    public void setPin(String pin) { this.pin = pin; }
}
