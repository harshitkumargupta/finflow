package com.finflow.service;

import com.finflow.entity.AutopayAuthorization;
import com.finflow.repository.AutopayAuthorizationRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.List;

@Service
@EnableScheduling
public class AutopayScheduler {

    private static final Logger log = LoggerFactory.getLogger(AutopayScheduler.class);

    private final AutopayAuthorizationRepository authorizationRepository;
    private final AutopayAuthorizationService autopayService;
    private final FinanceNotificationService notificationService;

    public AutopayScheduler(AutopayAuthorizationRepository authorizationRepository,
                            AutopayAuthorizationService autopayService,
                            FinanceNotificationService notificationService) {
        this.authorizationRepository = authorizationRepository;
        this.autopayService = autopayService;
        this.notificationService = notificationService;
    }

    // Run scheduled job every 6 hours
    @Scheduled(fixedDelayString = "${finflow.autopay.schedule-ms:21600000}", initialDelayString = "${finflow.autopay.initial-delay-ms:10000}")
    public void processScheduledAutopays() {
        LocalDate today = LocalDate.now();
        List<AutopayAuthorization> dueMandates = authorizationRepository.claimDue("ACTIVE", today, 50);

        if (dueMandates.isEmpty()) {
            return;
        }

        log.info("FinFlow AutopayScheduler processing {} due mandates", dueMandates.size());

        for (AutopayAuthorization mandate : dueMandates) {
            try {
                autopayService.executeScheduledMandate(mandate.getId());
            } catch (Exception e) {
                log.warn("Failed scheduled execution for mandate {}: {}", mandate.getId(), e.getMessage());
            }
        }
    }

    @Scheduled(fixedDelayString = "${finflow.notifications.schedule-ms:21600000}", initialDelayString = "${finflow.notifications.initial-delay-ms:30000}")
    public void processScheduledNotifications() {
        Long cursor = null;
        do {
            cursor = notificationService.generateForUserBatch(cursor);
        } while (cursor != null);
    }
}
