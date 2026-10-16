package com.finflow.controller;

import com.finflow.dto.PhoneVerificationStatusResponse;
import com.finflow.dto.VerifyPhoneRequest;
import com.finflow.entity.User;
import com.finflow.phone.OtpSender;
import com.finflow.service.CurrentUserService;
import com.finflow.service.PhoneVerificationService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class PhoneVerificationController {

    private final PhoneVerificationService verificationService;
    private final CurrentUserService currentUserService;
    private final OtpSender otpSender;

    public PhoneVerificationController(PhoneVerificationService verificationService,
                                       CurrentUserService currentUserService, OtpSender otpSender) {
        this.verificationService = verificationService;
        this.currentUserService = currentUserService;
        this.otpSender = otpSender;
    }

    @PostMapping("/api/users/me/phone-verification")
    public ResponseEntity<PhoneVerificationStatusResponse> send() {
        User user = currentUserService.get();
        if (verificationService.send(user) == PhoneVerificationService.SendOutcome.ALREADY_VERIFIED) {
            return ResponseEntity.ok(new PhoneVerificationStatusResponse("ALREADY_VERIFIED", true, false, "Your mobile number is already verified."));
        }
        boolean delivers = otpSender.delivers();
        String message = delivers
                ? "We sent a 6-digit code. Enter it below to verify your mobile number."
                : "Delivery isn't configured on this server, so the code was written to the server log instead of being sent.";
        return ResponseEntity.ok(new PhoneVerificationStatusResponse(delivers ? "SENT" : "LOGGED", false, delivers, message));
    }

    @PostMapping("/api/users/me/phone-verification/verify")
    public ResponseEntity<PhoneVerificationStatusResponse> verify(@Valid @RequestBody VerifyPhoneRequest request) {
        User user = currentUserService.get();
        PhoneVerificationService.VerifyOutcome outcome = verificationService.verify(user, request.code());
        return switch (outcome) {
            case VERIFIED -> ResponseEntity.ok(new PhoneVerificationStatusResponse("VERIFIED", true, false, "Your mobile number is verified."));
            case ALREADY_VERIFIED -> ResponseEntity.ok(new PhoneVerificationStatusResponse("ALREADY_VERIFIED", true, false, "Your mobile number is already verified."));
            case EXPIRED -> ResponseEntity.status(HttpStatus.BAD_REQUEST).body(new PhoneVerificationStatusResponse(
                    "EXPIRED", false, false, "This code has expired. Request a new one."));
            case LOCKED -> ResponseEntity.status(HttpStatus.BAD_REQUEST).body(new PhoneVerificationStatusResponse(
                    "LOCKED", false, false, "Too many incorrect attempts. Request a new code to try again."));
            case INVALID -> ResponseEntity.status(HttpStatus.BAD_REQUEST).body(new PhoneVerificationStatusResponse(
                    "INVALID", false, false, "That code is incorrect. Check the digits and try again."));
        };
    }
}
