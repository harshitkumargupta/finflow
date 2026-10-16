package com.finflow.dto;

public record EmailVerificationStatusResponse(String status, boolean emailVerified, boolean delivered, String message) {
}
