package com.finflow.email;

import com.finflow.entity.Wallet;
import com.finflow.repository.WalletRepository;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

@Component
public class EmailEventListener {

    private final EmailService emailService;
    private final WalletRepository walletRepository;

    public EmailEventListener(EmailService emailService, WalletRepository walletRepository) {
        this.emailService = emailService;
        this.walletRepository = walletRepository;
    }

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void onVerification(VerificationEmailRequested event) {
        emailService.sendVerification(event.to(), event.name(), event.rawToken(), event.ttlHours());
    }

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    @Transactional(propagation = Propagation.REQUIRES_NEW, readOnly = true)
    public void onWelcome(WelcomeEmailRequested event) {
        String paymentId = walletRepository.findByUserId(event.userId()).map(Wallet::getPaymentId).orElse(null);
        emailService.sendWelcome(event.to(), event.name(), event.accountNumber(), paymentId);
    }
}
