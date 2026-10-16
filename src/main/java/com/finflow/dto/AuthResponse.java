package com.finflow.dto;

public class AuthResponse {

    private String token;
    private String tokenType;

    public AuthResponse(String token) {
        this.token = token;
        this.tokenType = "Bearer";
    }

    public String getToken() {
        return token;
    }

    public String getTokenType() {
        return tokenType;
    }
}