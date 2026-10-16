package com.finflow.controller;

import com.finflow.dto.EmailVerificationStatusResponse;
import com.finflow.dto.VerifyEmailRequest;
import com.finflow.email.EmailService;
import com.finflow.entity.User;
import com.finflow.service.CurrentUserService;
import com.finflow.service.EmailVerificationService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class EmailVerificationController {

    private final EmailVerificationService verificationService;
    private final CurrentUserService currentUserService;
    private final EmailService emailService;

    public EmailVerificationController(EmailVerificationService verificationService,
                                       CurrentUserService currentUserService, EmailService emailService) {
        this.verificationService = verificationService;
        this.currentUserService = currentUserService;
        this.emailService = emailService;
    }

    @PostMapping("/api/auth/verify-email")
    public ResponseEntity<EmailVerificationStatusResponse> verify(@Valid @RequestBody VerifyEmailRequest request) {
        if (verificationService.verify(request.token())) {
            return ResponseEntity.ok(new EmailVerificationStatusResponse("VERIFIED", true, false, "Your email is verified."));
        }
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(new EmailVerificationStatusResponse(
                "INVALID_OR_EXPIRED", false, false, "This link is invalid, already used, or expired. Sign in and request a new one."));
    }

    @PostMapping("/api/users/me/email-verification")
    public ResponseEntity<EmailVerificationStatusResponse> resend() {
        User user = currentUserService.get();
        if (verificationService.resend(user) == EmailVerificationService.ResendOutcome.ALREADY_VERIFIED) {
            return ResponseEntity.ok(new EmailVerificationStatusResponse("ALREADY_VERIFIED", true, false, "Your email is already verified."));
        }
        boolean delivers = emailService.deliversMail();
        String message = delivers
                ? "Verification email sent. Check your inbox."
                : "Email isn't configured on this server, so the link was written to the server log instead of being sent.";
        return ResponseEntity.ok(new EmailVerificationStatusResponse(delivers ? "SENT" : "LOGGED", false, delivers, message));
    }
}
