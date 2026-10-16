package com.finflow.email;

import com.google.zxing.BarcodeFormat;
import com.google.zxing.EncodeHintType;
import com.google.zxing.client.j2se.MatrixToImageConfig;
import com.google.zxing.client.j2se.MatrixToImageWriter;
import com.google.zxing.common.BitMatrix;
import com.google.zxing.qrcode.QRCodeWriter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import com.finflow.dto.MonthlyBillLine;
import com.finflow.dto.MonthlyCategoryLine;
import com.finflow.dto.MonthlyGoalLine;
import com.finflow.dto.MonthlyReportResponse;
import org.springframework.web.util.HtmlUtils;

import java.io.ByteArrayOutputStream;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;
import java.util.Map;

@Service
public class EmailService {

    public static final String QR_PREFIX = "finflow:pay:v1:";
    private static final Logger log = LoggerFactory.getLogger(EmailService.class);

    private final EmailProvider provider;
    private final String baseUrl;

    public EmailService(EmailProvider provider, @Value("${finflow.app.base-url:http://localhost:5173}") String baseUrl) {
        this.provider = provider;
        this.baseUrl = baseUrl.endsWith("/") ? baseUrl.substring(0, baseUrl.length() - 1) : baseUrl;
    }

    public String providerName() {
        return provider.name();
    }

    public boolean deliversMail() {
        return provider.deliversMail();
    }

    public boolean sendVerification(String to, String name, String rawToken, long ttlHours) {
        String link = baseUrl + "/verify-email?token=" + rawToken;
        String first = firstName(name);
        String text = "Hi " + first + ",\n\nConfirm this email address for your FinFlow sandbox account:\n" + link
                + "\n\nThe link works once and expires in " + ttlHours + " hours. If you didn't sign up, ignore this email.\n\nFinFlow (sandbox: no real money moves)";
        String html = layout("Confirm your email",
                "<p style=\"margin:0 0 16px\">Hi " + HtmlUtils.htmlEscape(first) + ",</p>"
                        + "<p style=\"margin:0 0 24px\">Confirm this email address for your FinFlow sandbox account.</p>"
                        + "<p style=\"margin:0 0 24px\"><a href=\"" + HtmlUtils.htmlEscape(link) + "\" style=\"display:inline-block;background:#2A7351;color:#FBF8F1;text-decoration:none;font-weight:600;padding:12px 20px;border-radius:10px\">Verify email</a></p>"
                        + "<p style=\"margin:0;color:#5C635B;font-size:13px\">The link works once and expires in " + ttlHours + " hours. If you didn't sign up, you can ignore this email.</p>");
        return deliver(new EmailMessage(to, "Confirm your email for FinFlow", text, html, List.of()));
    }

    public boolean sendPhoneOtp(String to, String name, String code, long ttlMinutes) {
        String first = firstName(name);
        String text = "Hi " + first + ",\n\nYour FinFlow phone verification code is " + code
                + "\n\nEnter it in FinFlow to confirm your mobile number. The code expires in " + ttlMinutes
                + " minutes. If you didn't request it, you can ignore this email.\n\nFinFlow will never ask for this code by phone or message.\n\nFinFlow (sandbox: no real money moves)";
        String html = layout("Your phone verification code",
                "<p style=\"margin:0 0 16px\">Hi " + HtmlUtils.htmlEscape(first) + ",</p>"
                        + "<p style=\"margin:0 0 20px\">Enter this code in FinFlow to confirm your mobile number.</p>"
                        + "<p style=\"margin:0 0 20px;font-size:34px;font-weight:700;letter-spacing:.32em;font-variant-numeric:tabular-nums;color:#2A7351\">" + HtmlUtils.htmlEscape(code) + "</p>"
                        + "<p style=\"margin:0 0 8px;color:#5C635B;font-size:13px\">The code expires in " + ttlMinutes + " minutes. If you didn't request it, you can ignore this email.</p>"
                        + "<p style=\"margin:0;color:#5C635B;font-size:13px\">FinFlow will never ask for this code by phone or message.</p>");
        return deliver(new EmailMessage(to, "Your FinFlow phone verification code", text, html, List.of()));
    }

    public boolean sendMonthlyReport(String to, String name, MonthlyReportResponse report) {
        String first = firstName(name);
        StringBuilder text = new StringBuilder("Hi ").append(first).append(",\n\n").append(report.message()).append("\n\n");
        StringBuilder html = new StringBuilder()
                .append("<p style=\"margin:0 0 16px\">Hi ").append(HtmlUtils.htmlEscape(first)).append(",</p>");

        if (!report.hasActivity()) {
            text.append("There was no account activity to summarise.\n");
            html.append("<p style=\"margin:0 0 20px\">There was no account activity to summarise for ").append(HtmlUtils.htmlEscape(report.monthLabel())).append(".</p>");
        } else {
            html.append("<p style=\"margin:0 0 8px;color:#5C635B\">").append(HtmlUtils.htmlEscape(report.narration())).append("</p>");
            text.append(report.narration()).append("\n\n");

            html.append("<table role=\"presentation\" style=\"border-collapse:collapse;margin:16px 0 20px\">");
            html.append(row("Money in", money(report.totalIn())));
            html.append(row("Money out", money(report.totalOut())));
            html.append(row("Net", money(report.net())));
            html.append(row("Transactions", String.valueOf(report.transactionCount())));
            html.append("</table>");
            text.append("Money in: ").append(money(report.totalIn())).append('\n')
                    .append("Money out: ").append(money(report.totalOut())).append('\n')
                    .append("Net: ").append(money(report.net())).append('\n')
                    .append("Transactions: ").append(report.transactionCount()).append('\n');

            if (!report.topCategories().isEmpty()) {
                html.append("<p style=\"margin:0 0 6px;font-weight:600\">Top categories</p><ul style=\"margin:0 0 20px;padding-left:18px;color:#5C635B\">");
                text.append("\nTop categories:\n");
                for (MonthlyCategoryLine line : report.topCategories()) {
                    html.append("<li>").append(HtmlUtils.htmlEscape(label(line.category()))).append(" — ").append(money(line.amount())).append("</li>");
                    text.append("  ").append(label(line.category())).append(" - ").append(money(line.amount())).append('\n');
                }
                html.append("</ul>");
            }

            if (!report.bills().isEmpty()) {
                html.append("<p style=\"margin:0 0 6px;font-weight:600\">Bills due this month</p><ul style=\"margin:0 0 20px;padding-left:18px;color:#5C635B\">");
                text.append("\nBills due this month:\n");
                for (MonthlyBillLine line : report.bills()) {
                    html.append("<li>").append(HtmlUtils.htmlEscape(line.name())).append(" — ").append(money(line.amount())).append("</li>");
                    text.append("  ").append(line.name()).append(" - ").append(money(line.amount())).append('\n');
                }
                html.append("</ul>");
            }
            if (report.activeAutopayCount() > 0) {
                html.append("<p style=\"margin:0 0 20px;color:#5C635B\">Active autopay: ").append(report.activeAutopayCount()).append(" mandate(s), ").append(money(report.activeAutopayAmount())).append(" total.</p>");
                text.append("\nActive autopay: ").append(report.activeAutopayCount()).append(" mandate(s), ").append(money(report.activeAutopayAmount())).append(" total.\n");
            }
            if (!report.goals().isEmpty()) {
                html.append("<p style=\"margin:0 0 6px;font-weight:600\">Savings goals</p><ul style=\"margin:0 0 20px;padding-left:18px;color:#5C635B\">");
                text.append("\nSavings goals:\n");
                for (MonthlyGoalLine line : report.goals()) {
                    html.append("<li>").append(HtmlUtils.htmlEscape(line.name())).append(" — ").append(money(line.currentAmount())).append(" of ").append(money(line.targetAmount())).append(" (").append(line.percentComplete()).append("%)</li>");
                    text.append("  ").append(line.name()).append(" - ").append(money(line.currentAmount())).append(" of ").append(money(line.targetAmount())).append(" (").append(line.percentComplete()).append("%)\n");
                }
                html.append("</ul>");
            }
        }
        text.append("\nFinFlow (sandbox: no real money moves)");
        html.append("<p style=\"margin:16px 0 0;color:#5C635B;font-size:13px\">FinFlow (sandbox: no real money moves).</p>");
        return deliver(new EmailMessage(to, "Your FinFlow report for " + report.monthLabel(), text.toString(), layout("Your " + report.monthLabel() + " report", html.toString()), List.of()));
    }

    private static String row(String label, String value) {
        return "<tr><td style=\"padding:4px 16px 4px 0;color:#5C635B\">" + HtmlUtils.htmlEscape(label)
                + "</td><td style=\"padding:4px 0;font-weight:600;font-variant-numeric:tabular-nums\">" + HtmlUtils.htmlEscape(value) + "</td></tr>";
    }

    private static String money(BigDecimal value) {
        if (value == null) return "₹0.00";
        return "₹" + value.setScale(2, RoundingMode.HALF_UP).toPlainString();
    }

    private static String label(String category) {
        if (category == null || category.isBlank()) return "Other";
        String lower = category.toLowerCase().replace('_', ' ');
        return Character.toUpperCase(lower.charAt(0)) + lower.substring(1);
    }

    public boolean sendWelcome(String to, String name, String accountNumber, String paymentId) {
        String first = firstName(name);
        String masked = maskAccount(accountNumber);
        boolean hasIdentity = paymentId != null && !paymentId.isBlank();
        StringBuilder text = new StringBuilder("Hi ").append(first).append(",\n\nYour FinFlow sandbox account is open.\nAccount: ").append(masked).append('\n');
        StringBuilder html = new StringBuilder()
                .append("<p style=\"margin:0 0 16px\">Hi ").append(HtmlUtils.htmlEscape(first)).append(",</p>")
                .append("<p style=\"margin:0 0 20px\">Your FinFlow sandbox account is open.</p>")
                .append("<table role=\"presentation\" style=\"border-collapse:collapse;margin:0 0 24px\"><tr><td style=\"padding:6px 16px 6px 0;color:#5C635B\">Account</td><td style=\"padding:6px 0;font-weight:600;font-variant-numeric:tabular-nums\">")
                .append(HtmlUtils.htmlEscape(masked)).append("</td></tr></table>");
        List<InlineImage> images = List.of();
        if (hasIdentity) {
            byte[] qr = qrPng(qrPayload(paymentId));
            text.append("\nTo receive money, share your FinFlow Payment ID: ").append(paymentId).append('\n');
            html.append("<p style=\"margin:0 0 12px;font-weight:600\">Receive money</p>")
                    .append("<p style=\"margin:0 0 12px;color:#5C635B;font-size:14px\">Anyone with FinFlow can scan this to pay you. It only contains your public Payment ID.</p>")
                    .append("<img src=\"cid:receive-qr\" width=\"180\" height=\"180\" alt=\"QR code for Payment ID ").append(HtmlUtils.htmlEscape(paymentId)).append("\" style=\"display:block;margin:0 0 8px;border:1px solid #E1D9C9;border-radius:10px\" />")
                    .append("<p style=\"margin:0 0 24px;font-variant-numeric:tabular-nums\">").append(HtmlUtils.htmlEscape(paymentId)).append("</p>");
            images = List.of(new InlineImage("receive-qr", "image/png", qr));
        }
        if (!hasIdentity) {
            text.append("\nTo receive money, open Wallet & pay in FinFlow. Your receive-money QR appears there once your wallet is set up.\n");
            html.append("<p style=\"margin:0 0 24px;color:#5C635B;font-size:14px\">To receive money, open <strong>Wallet &amp; pay</strong> in FinFlow. Your receive-money QR appears there once your wallet is set up.</p>");
        }
        text.append("\nFinFlow will never ask for your PIN, password or card details by email.\n\nFinFlow (sandbox: no real money moves)");
        html.append("<p style=\"margin:0;color:#5C635B;font-size:13px\">FinFlow will never ask for your PIN, password or card details by email.</p>");
        return deliver(new EmailMessage(to, "Welcome to FinFlow", text.toString(), layout("Welcome to FinFlow", html.toString()), images));
    }

    boolean deliver(EmailMessage message) {
        try {
            provider.send(message);
            return true;
        } catch (Exception exception) {
            log.warn("Email delivery failed via {} to {}: {}", provider.name(), maskEmail(message.to()), exception.getMessage());
            return false;
        }
    }

    public static String qrPayload(String paymentId) {
        return QR_PREFIX + paymentId;
    }

    public static byte[] qrPng(String payload) {
        try {
            BitMatrix matrix = new QRCodeWriter().encode(payload, BarcodeFormat.QR_CODE, 360, 360, Map.of(EncodeHintType.MARGIN, 2));
            ByteArrayOutputStream out = new ByteArrayOutputStream();
            MatrixToImageWriter.writeToStream(matrix, "PNG", out, new MatrixToImageConfig(0xFF1E2620, 0xFFFFFFFF));
            return out.toByteArray();
        } catch (Exception exception) {
            throw new IllegalStateException("QR generation failed", exception);
        }
    }

    public static String maskAccount(String accountNumber) {
        if (accountNumber == null || accountNumber.isBlank()) return "••••";
        String trimmed = accountNumber.trim();
        return "•••• " + trimmed.substring(Math.max(0, trimmed.length() - 4));
    }

    public static String maskPhone(String phone) {
        if (phone == null || phone.isBlank()) return "••••";
        String trimmed = phone.trim();
        return "•••• " + trimmed.substring(Math.max(0, trimmed.length() - 4));
    }

    public static String maskEmail(String email) {
        if (email == null || !email.contains("@")) return "***";
        int at = email.indexOf('@');
        return (at <= 1 ? "*" : email.charAt(0) + "***") + email.substring(at);
    }

    private static String firstName(String name) {
        if (name == null || name.isBlank()) return "there";
        return name.trim().split("\\s+")[0];
    }

    private static String layout(String title, String body) {
        return "<!doctype html><html><body style=\"margin:0;background:#F3EEE4;font-family:-apple-system,Segoe UI,Helvetica,Arial,sans-serif;color:#1E2620\">"
                + "<table role=\"presentation\" width=\"100%\" style=\"border-collapse:collapse\"><tr><td style=\"padding:32px 16px\">"
                + "<table role=\"presentation\" style=\"max-width:520px;margin:0 auto;border-collapse:collapse;background:#FBF8F1;border:1px solid #E1D9C9;border-radius:16px\"><tr><td style=\"padding:32px\">"
                + "<p style=\"margin:0 0 4px;font-size:12px;letter-spacing:.14em;text-transform:uppercase;color:#5C635B\">FinFlow</p>"
                + "<h1 style=\"margin:0 0 24px;font-family:Georgia,serif;font-weight:500;font-size:26px\">" + HtmlUtils.htmlEscape(title) + "</h1>"
                + body
                + "</td></tr></table>"
                + "<p style=\"max-width:520px;margin:16px auto 0;text-align:center;color:#5C635B;font-size:12px\">Sandbox: no real money moves.</p>"
                + "</td></tr></table></body></html>";
    }
}
