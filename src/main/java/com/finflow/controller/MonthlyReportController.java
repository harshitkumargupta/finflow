package com.finflow.controller;

import com.finflow.dto.MonthlyReportEmailResponse;
import com.finflow.dto.MonthlyReportResponse;
import com.finflow.email.EmailService;
import com.finflow.entity.User;
import com.finflow.service.CurrentUserService;
import com.finflow.service.MonthlyReportService;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.time.YearMonth;
import java.time.format.DateTimeParseException;

@RestController
@RequestMapping("/api/reports/monthly")
public class MonthlyReportController {

    private final MonthlyReportService reportService;
    private final CurrentUserService currentUserService;
    private final EmailService emailService;

    public MonthlyReportController(MonthlyReportService reportService, CurrentUserService currentUserService, EmailService emailService) {
        this.reportService = reportService;
        this.currentUserService = currentUserService;
        this.emailService = emailService;
    }

    @GetMapping
    public MonthlyReportResponse get(@RequestParam(required = false) String month) {
        User user = currentUserService.get();
        return reportService.build(user, resolveMonth(month));
    }

    @PostMapping("/email")
    public MonthlyReportEmailResponse email(@RequestParam(required = false) String month) {
        User user = currentUserService.get();
        YearMonth target = resolveMonth(month);
        MonthlyReportResponse report = reportService.build(user, target);
        emailService.sendMonthlyReport(user.getEmail(), user.getName(), report);
        boolean delivers = emailService.deliversMail();
        String message = delivers
                ? "Your " + report.monthLabel() + " report has been emailed to " + user.getEmail() + "."
                : "Email isn't configured on this server, so the report was written to the server log instead of being sent.";
        return new MonthlyReportEmailResponse(delivers ? "SENT" : "LOGGED", delivers, report.month(), message);
    }

    private YearMonth resolveMonth(String month) {
        if (month == null || month.isBlank()) {
            return YearMonth.now().minusMonths(1);
        }
        try {
            return YearMonth.parse(month.trim());
        } catch (DateTimeParseException exception) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "month must be in YYYY-MM format");
        }
    }
}
