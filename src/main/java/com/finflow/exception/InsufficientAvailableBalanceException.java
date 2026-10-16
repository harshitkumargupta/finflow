package com.finflow.exception;

public class InsufficientAvailableBalanceException extends RuntimeException {
    public InsufficientAvailableBalanceException(String message) {
        super(message);
    }
}
