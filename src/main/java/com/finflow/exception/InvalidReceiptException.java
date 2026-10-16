package com.finflow.exception;

public class InvalidReceiptException extends RuntimeException {
    public InvalidReceiptException(String message) {
        super(message);
    }
}
