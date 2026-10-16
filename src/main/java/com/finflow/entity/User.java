package com.finflow.entity;

import jakarta.persistence.*;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import java.time.LocalDateTime;

@Entity
@Table(name = "users")
public class User {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    @NotBlank(message = "Name is required")
    private String name;

    @Column(nullable = false, unique = true)
    @NotBlank(message = "Email is required")
    @Email(message = "Invalid email format")
    private String email;

    @Column(nullable = false, unique = true)
    @NotBlank(message = "Phone is required")
    @Pattern(regexp = "^[0-9]{10}$", message = "Phone must be a 10-digit number")
    private String phone;

    @Column(nullable = false)
    @NotBlank(message = "Password is required")
    @Size(min = 8, message = "Password must contain at least 8 characters")
    private String password;

    @Column(nullable = false)
    private LocalDateTime createdAt;

    @Column(name = "transaction_pin_hash", length = 100)
    private String transactionPinHash;

    @Column(nullable = false, length = 20, columnDefinition = "varchar(20) default 'USER'")
    private String role = "USER";

    @Column(nullable = false, columnDefinition = "integer default 0")
    private int failedPinAttempts;

    private LocalDateTime pinLockedUntil;

    @Column(name = "email_verified", nullable = false, columnDefinition = "boolean default false")
    private boolean emailVerified;

    @Column(name = "email_verified_at")
    private LocalDateTime emailVerifiedAt;

    @Column(name = "phone_verified", nullable = false, columnDefinition = "boolean default false")
    private boolean phoneVerified;

    @Column(name = "phone_verified_at")
    private LocalDateTime phoneVerifiedAt;

    public User() {
    }

    public User(String name, String email, String phone,
                String password, LocalDateTime createdAt) {
        this.name = name;
        this.email = email;
        this.phone = phone;
        this.password = password;
        this.createdAt = createdAt;
    }

    public Long getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getPhone() {
        return phone;
    }

    public void setPhone(String phone) {
        this.phone = phone;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public String getTransactionPinHash() {
        return transactionPinHash;
    }

    public void setTransactionPinHash(String transactionPinHash) {
        this.transactionPinHash = transactionPinHash;
    }

    public String getRole() { return role; }
    public void setRole(String role) { this.role = role; }
    public int getFailedPinAttempts() { return failedPinAttempts; }
    public void setFailedPinAttempts(int failedPinAttempts) { this.failedPinAttempts = failedPinAttempts; }
    public LocalDateTime getPinLockedUntil() { return pinLockedUntil; }
    public void setPinLockedUntil(LocalDateTime pinLockedUntil) { this.pinLockedUntil = pinLockedUntil; }
    public boolean isEmailVerified() { return emailVerified; }
    public void setEmailVerified(boolean emailVerified) { this.emailVerified = emailVerified; }
    public LocalDateTime getEmailVerifiedAt() { return emailVerifiedAt; }
    public void setEmailVerifiedAt(LocalDateTime emailVerifiedAt) { this.emailVerifiedAt = emailVerifiedAt; }
    public boolean isPhoneVerified() { return phoneVerified; }
    public void setPhoneVerified(boolean phoneVerified) { this.phoneVerified = phoneVerified; }
    public LocalDateTime getPhoneVerifiedAt() { return phoneVerifiedAt; }
    public void setPhoneVerifiedAt(LocalDateTime phoneVerifiedAt) { this.phoneVerifiedAt = phoneVerifiedAt; }
}
