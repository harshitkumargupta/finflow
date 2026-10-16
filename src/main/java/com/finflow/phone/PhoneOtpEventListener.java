package com.finflow.phone;

import com.finflow.entity.User;
import com.finflow.exception.ResourceNotFoundException;
import com.finflow.repository.UserRepository;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

@Component
public class PhoneOtpEventListener {

    private final OtpSender otpSender;
    private final UserRepository userRepository;

    public PhoneOtpEventListener(OtpSender otpSender, UserRepository userRepository) {
        this.otpSender = otpSender;
        this.userRepository = userRepository;
    }

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    @Transactional(propagation = Propagation.REQUIRES_NEW, readOnly = true)
    public void onPhoneOtpRequested(PhoneOtpRequested event) {
        User user = userRepository.findById(event.userId())
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));
        otpSender.sendOtp(user, event.code(), event.ttlMinutes());
    }
}
