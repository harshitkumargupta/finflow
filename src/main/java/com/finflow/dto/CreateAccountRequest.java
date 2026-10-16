package com.finflow.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public class CreateAccountRequest {

    @NotBlank(message = "Account type is required")
    @Pattern(
            regexp = "FINFLOW|SAVINGS|CURRENT",
            message = "Account type must be FINFLOW"
    )
    private String accountType;

    @NotBlank(message = "Sandbox identity confirmation is required")
    @Pattern(regexp = "CONFIRM", message = "Type CONFIRM to complete sandbox identity verification")
    private String sandboxIdentityConfirmation;

    @NotBlank(message = "Transaction PIN is required")
    @Pattern(regexp = "^[0-9]{6}$", message = "Transaction PIN must be exactly 6 digits")
    private String transactionPin;

    @NotBlank(message = "Transaction PIN confirmation is required")
    @Size(min = 6, max = 6, message = "Transaction PIN confirmation must be exactly 6 digits")
    private String confirmTransactionPin;

    public CreateAccountRequest() {
    }

    public String getAccountType() {
        return accountType;
    }

    public void setAccountType(String accountType) {
        this.accountType = accountType;
    }

    public String getSandboxIdentityConfirmation() {
        return sandboxIdentityConfirmation;
    }

    public void setSandboxIdentityConfirmation(String sandboxIdentityConfirmation) {
        this.sandboxIdentityConfirmation = sandboxIdentityConfirmation;
    }

    public String getTransactionPin() {
        return transactionPin;
    }

    public void setTransactionPin(String transactionPin) {
        this.transactionPin = transactionPin;
    }

    public String getConfirmTransactionPin() {
        return confirmTransactionPin;
    }

    public void setConfirmTransactionPin(String confirmTransactionPin) {
        this.confirmTransactionPin = confirmTransactionPin;
    }
}
