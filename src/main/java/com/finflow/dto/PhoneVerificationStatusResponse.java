package com.finflow.dto;

public record PhoneVerificationStatusResponse(String status, boolean phoneVerified, boolean delivered, String message) {
}
