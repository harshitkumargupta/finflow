package com.finflow.exception;

public class SpendingLimitExceededException extends RuntimeException {
    public SpendingLimitExceededException(String message) {
        super(message);
    }
}
