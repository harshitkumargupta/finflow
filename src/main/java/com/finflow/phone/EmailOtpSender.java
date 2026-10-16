package com.finflow.phone;

import com.finflow.email.EmailService;
import com.finflow.entity.User;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

@Component
@ConditionalOnProperty(name = "finflow.phone.channel", havingValue = "EMAIL", matchIfMissing = true)
public class EmailOtpSender implements OtpSender {

    private static final Logger log = LoggerFactory.getLogger(EmailOtpSender.class);

    private final EmailService emailService;

    public EmailOtpSender(EmailService emailService) {
        this.emailService = emailService;
    }

    @Override
    public String channel() {
        return "EMAIL";
    }

    @Override
    public boolean delivers() {
        return emailService.deliversMail();
    }

    @Override
    public void sendOtp(User user, String code, long ttlMinutes) {
        log.info("Phone verification OTP (sandbox) for phone {}: code={} ttlMinutes={}",
                EmailService.maskPhone(user.getPhone()), code, ttlMinutes);
        emailService.sendPhoneOtp(user.getEmail(), user.getName(), code, ttlMinutes);
    }
}
