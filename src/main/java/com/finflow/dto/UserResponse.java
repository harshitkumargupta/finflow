package com.finflow.dto;

import java.time.LocalDateTime;

public class UserResponse {

    private Long id;
    private String name;
    private String email;
    private String phone;
    private LocalDateTime createdAt;
    private boolean emailVerified;
    private boolean phoneVerified;

    public UserResponse() {
    }

    public UserResponse(Long id, String name, String email,
                        String phone, LocalDateTime createdAt) {
        this.id = id;
        this.name = name;
        this.email = email;
        this.phone = phone;
        this.createdAt = createdAt;
    }

    public UserResponse(Long id, String name, String email, String phone, LocalDateTime createdAt, boolean emailVerified) {
        this(id, name, email, phone, createdAt);
        this.emailVerified = emailVerified;
    }

    public UserResponse(Long id, String name, String email, String phone, LocalDateTime createdAt, boolean emailVerified, boolean phoneVerified) {
        this(id, name, email, phone, createdAt, emailVerified);
        this.phoneVerified = phoneVerified;
    }

    public boolean isEmailVerified() { return emailVerified; }

    public boolean isPhoneVerified() { return phoneVerified; }

    public Long getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public String getEmail() {
        return email;
    }

    public String getPhone() {
        return phone;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }
}