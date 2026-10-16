package com.finflow.exception;

public class MarketProviderUnavailableException extends RuntimeException {
    public MarketProviderUnavailableException(String message) {
        super(message);
    }

    public MarketProviderUnavailableException(String message, Throwable cause) {
        super(message, cause);
    }
}
