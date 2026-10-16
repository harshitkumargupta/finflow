package com.finflow.exception;

public class InvalidPageRequestException extends RuntimeException {
    public InvalidPageRequestException(String message) {
        super(message);
    }
}
