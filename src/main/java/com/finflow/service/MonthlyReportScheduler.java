package com.finflow.service;

import com.finflow.dto.MonthlyReportResponse;
import com.finflow.email.EmailService;
import com.finflow.entity.User;
import com.finflow.repository.UserRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.PageRequest;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.time.YearMonth;
import java.util.List;

@Service
public class MonthlyReportScheduler {

    private static final Logger log = LoggerFactory.getLogger(MonthlyReportScheduler.class);
    private static final int BATCH_SIZE = 100;

    private final UserRepository userRepository;
    private final MonthlyReportService reportService;
    private final EmailService emailService;
    private final boolean enabled;

    public MonthlyReportScheduler(UserRepository userRepository, MonthlyReportService reportService,
                                  EmailService emailService,
                                  @Value("${finflow.reports.monthly.enabled:true}") boolean enabled) {
        this.userRepository = userRepository;
        this.reportService = reportService;
        this.emailService = emailService;
        this.enabled = enabled;
    }

    @Scheduled(cron = "${finflow.reports.monthly.cron:0 0 7 1 * *}", zone = "${finflow.reports.monthly.zone:UTC}")
    public void sendMonthlyReports() {
        if (!enabled) return;
        YearMonth previousMonth = YearMonth.now().minusMonths(1);
        int sent = dispatch(previousMonth);
        log.info("FinFlow MonthlyReportScheduler emailed {} report(s) for {}", sent, previousMonth);
    }

    public int dispatch(YearMonth month) {
        int sent = 0;
        long cursor = 0;
        while (true) {
            List<User> batch = userRepository.findByIdGreaterThanOrderByIdAsc(cursor, PageRequest.ofSize(BATCH_SIZE));
            if (batch.isEmpty()) break;
            for (User user : batch) {
                cursor = user.getId();
                if (!user.isEmailVerified()) continue;
                try {
                    MonthlyReportResponse report = reportService.build(user, month);
                    if (!report.hasActivity()) continue;
                    emailService.sendMonthlyReport(user.getEmail(), user.getName(), report);
                    sent++;
                } catch (Exception exception) {
                    log.warn("Monthly report failed for user {}: {}", user.getId(), exception.getMessage());
                }
            }
        }
        return sent;
    }
}
