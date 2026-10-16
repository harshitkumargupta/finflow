package com.finflow.phone;

public record PhoneOtpRequested(Long userId, String code, long ttlMinutes) {
}
