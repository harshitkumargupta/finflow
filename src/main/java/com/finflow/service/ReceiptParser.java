package com.finflow.service;

import com.finflow.receipt.OcrResult;
import com.finflow.receipt.OcrWord;
import com.finflow.receipt.ParsedField;
import com.finflow.receipt.ParsedReceipt;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Service
public class ReceiptParser {

    private static final int CONFIDENCE_THRESHOLD = 60;

    private static final Pattern MONEY = Pattern.compile("(₹|rs\\.?|inr)?\\s*([0-9][0-9,]*(?:\\.[0-9]{1,2})?)", Pattern.CASE_INSENSITIVE);
    private static final int MAX_INTEGER_DIGITS = 9;
    private static final int MAX_PLAIN_INTEGER_DIGITS = 6;
    private static final List<String> STRONG_TOTAL_KEYS = List.of("grand total", "amount payable", "amount due", "balance due", "net payable", "net amount", "total payable", "total amount", "amount paid", "total paid", "you paid", "total");
    private static final List<String> EXCLUDE_TOTAL_KEYS = List.of("subtotal", "sub total", "tax", "gst", "cgst", "sgst", "vat", "change", "tendered", "cash", "round");
    private static final List<String> MERCHANT_EXCLUDE = List.of("receipt", "invoice", "bill", "tax invoice", "gstin", "gst no", "phone", "tel", "www", "http", "date", "time", "order", "table", "cashier",
            "paytm", "phonepe", "google pay", "gpay", "bhim", "upi id", "upi ref", "ref no", "powered by", "scratchcard", "cashback", "check balance", "pay again", "payment successful", "transaction successful", "successful", "amazon pay");

    private static final Pattern UPI_ID = Pattern.compile("([a-zA-Z0-9][a-zA-Z0-9.\\-_]{1,}@[a-zA-Z][a-zA-Z0-9.\\-]{1,})");

    private static final Pattern DATE_ISO = Pattern.compile("\\b(\\d{4})[-/](\\d{1,2})[-/](\\d{1,2})\\b");
    private static final Pattern DATE_DMY4 = Pattern.compile("\\b(\\d{1,2})[-/.](\\d{1,2})[-/.](\\d{4})\\b");
    private static final Pattern DATE_DMY2 = Pattern.compile("\\b(\\d{1,2})[-/.](\\d{1,2})[-/.](\\d{2})\\b");
    private static final Pattern DATE_DMON = Pattern.compile("\\b(\\d{1,2})[ -]([A-Za-z]{3,9})[ -](\\d{4})\\b");
    private static final Pattern DATE_MOND = Pattern.compile("\\b([A-Za-z]{3,9})\\s+(\\d{1,2}),?\\s+(\\d{4})\\b");

    public ParsedReceipt parse(OcrResult ocr) {
        String text = ocr == null || ocr.text() == null ? "" : ocr.text();
        List<String> lines = text.lines().map(String::trim).filter(line -> !line.isBlank()).toList();
        List<OcrWord> words = ocr == null ? List.of() : ocr.words();
        double fallbackConfidence = ocr == null ? 0 : ocr.meanConfidence();
        return new ParsedReceipt(
                parseMerchant(lines, words, fallbackConfidence),
                parseDate(text, words, fallbackConfidence),
                parseTotal(lines, words, fallbackConfidence),
                parseUpiId(text, words, fallbackConfidence));
    }

    private ParsedField<BigDecimal> parseTotal(List<String> lines, List<OcrWord> words, double fallbackConfidence) {
        BigDecimal currencyStrong = null;
        BigDecimal strongPlain = null;
        BigDecimal topCurrency = null;
        BigDecimal topPlain = null;
        BigDecimal anyPlain = null;
        int topLimit = Math.max(6, (int) Math.ceil(lines.size() * 0.4));
        boolean previousStrongWithoutAmount = false;
        for (int index = 0; index < lines.size(); index++) {
            String line = lines.get(index);
            String lower = line.toLowerCase(Locale.ROOT);
            boolean excluded = EXCLUDE_TOTAL_KEYS.stream().anyMatch(lower::contains);
            boolean strongKey = !excluded && STRONG_TOTAL_KEYS.stream().anyMatch(lower::contains);
            List<Candidate> candidates = amountsOf(line);
            boolean effectiveStrong = strongKey || previousStrongWithoutAmount;
            boolean inTop = index < topLimit;
            for (Candidate candidate : candidates) {
                if (effectiveStrong) {
                    if (candidate.hasCurrency()) currencyStrong = max(currencyStrong, candidate.value());
                    else strongPlain = max(strongPlain, candidate.value());
                }
                if (inTop) {
                    if (candidate.hasCurrency()) topCurrency = max(topCurrency, candidate.value());
                    else topPlain = max(topPlain, candidate.value());
                }
                if (!candidate.hasCurrency()) anyPlain = max(anyPlain, candidate.value());
            }
            previousStrongWithoutAmount = strongKey && candidates.isEmpty();
        }
        boolean reliable = currencyStrong != null || strongPlain != null;
        BigDecimal chosen = firstNonNull(currencyStrong, strongPlain, topCurrency, topPlain, anyPlain);
        if (chosen == null) return ParsedField.missing();
        int confidence = confidenceForToken(chosen.toPlainString(), words, fallbackConfidence);
        boolean flagged = !reliable || confidence < CONFIDENCE_THRESHOLD;
        return new ParsedField<>(chosen, confidence, flagged);
    }

    private ParsedField<String> parseUpiId(String text, List<OcrWord> words, double fallbackConfidence) {
        Matcher matcher = UPI_ID.matcher(text);
        while (matcher.find()) {
            String candidate = matcher.group(1);
            if (candidate.contains("@") && !candidate.matches(".*@(gmail|yahoo|outlook|hotmail|icloud)\\..*")) {
                int confidence = confidenceForToken(candidate.substring(0, Math.min(candidate.length(), candidate.indexOf('@'))), words, fallbackConfidence);
                return new ParsedField<>(candidate, confidence, confidence < CONFIDENCE_THRESHOLD);
            }
        }
        return ParsedField.missing();
    }

    private List<Candidate> amountsOf(String line) {
        Matcher matcher = MONEY.matcher(line);
        List<Candidate> candidates = new ArrayList<>();
        while (matcher.find()) {
            int digitStart = matcher.start(2);
            int digitEnd = matcher.end(2);
            char before = digitStart > 0 ? line.charAt(digitStart - 1) : ' ';
            char after = digitEnd < line.length() ? line.charAt(digitEnd) : ' ';
            if (before == '*' || before == 'x' || before == 'X' || before == '#') continue;
            if (before == ':' || after == ':' || before == '%' || after == '%') continue;
            boolean hasCurrency = matcher.group(1) != null;
            String digits = matcher.group(2).replace(",", "");
            boolean hasDecimal = digits.contains(".");
            int integerDigits = (hasDecimal ? digits.substring(0, digits.indexOf('.')) : digits).length();
            if (integerDigits > MAX_INTEGER_DIGITS) continue;
            if (!hasCurrency && !hasDecimal && integerDigits > MAX_PLAIN_INTEGER_DIGITS) continue;
            try {
                BigDecimal value = new BigDecimal(digits);
                if (value.signum() > 0) candidates.add(new Candidate(value.setScale(2, RoundingMode.HALF_UP), hasCurrency));
            } catch (NumberFormatException ignored) {
            }
        }
        return candidates;
    }

    private BigDecimal max(BigDecimal current, BigDecimal value) {
        if (current == null) return value;
        return value.compareTo(current) > 0 ? value : current;
    }

    @SafeVarargs
    private <T> T firstNonNull(T... values) {
        for (T value : values) {
            if (value != null) return value;
        }
        return null;
    }

    private record Candidate(BigDecimal value, boolean hasCurrency) {
    }

    private ParsedField<LocalDate> parseDate(String text, List<OcrWord> words, double fallbackConfidence) {
        Matcher iso = DATE_ISO.matcher(text);
        if (iso.find()) {
            LocalDate date = build(toInt(iso.group(3)), toInt(iso.group(2)), toInt(iso.group(1)));
            if (date != null) return dateField(date, iso.group(1), words, fallbackConfidence);
        }
        for (Pattern pattern : List.of(DATE_DMY4, DATE_DMY2)) {
            Matcher matcher = pattern.matcher(text);
            while (matcher.find()) {
                int first = toInt(matcher.group(1));
                int second = toInt(matcher.group(2));
                int year = normalizeYear(toInt(matcher.group(3)));
                int day = first;
                int month = second;
                if (first <= 12 && second > 12) {
                    month = first;
                    day = second;
                }
                LocalDate date = build(day, month, year);
                if (date != null) return dateField(date, matcher.group(1), words, fallbackConfidence);
            }
        }
        Matcher dmon = DATE_DMON.matcher(text);
        if (dmon.find()) {
            int month = monthFromName(dmon.group(2));
            LocalDate date = month == 0 ? null : build(toInt(dmon.group(1)), month, toInt(dmon.group(3)));
            if (date != null) return dateField(date, dmon.group(1), words, fallbackConfidence);
        }
        Matcher mond = DATE_MOND.matcher(text);
        if (mond.find()) {
            int month = monthFromName(mond.group(1));
            LocalDate date = month == 0 ? null : build(toInt(mond.group(2)), month, toInt(mond.group(3)));
            if (date != null) return dateField(date, mond.group(1), words, fallbackConfidence);
        }
        return ParsedField.missing();
    }

    private ParsedField<String> parseMerchant(List<String> lines, List<OcrWord> words, double fallbackConfidence) {
        int limit = Math.min(lines.size(), 6);
        for (int index = 0; index < limit; index++) {
            String line = lines.get(index);
            String lower = line.toLowerCase(Locale.ROOT);
            if (MERCHANT_EXCLUDE.stream().anyMatch(lower::contains)) continue;
            long letters = line.chars().filter(Character::isLetter).count();
            long digits = line.chars().filter(Character::isDigit).count();
            if (letters < 3 || digits > letters || line.length() > 48) continue;
            String merchant = line.replaceAll("\\s{2,}", " ").trim();
            int confidence = confidenceForToken(firstWord(merchant), words, fallbackConfidence);
            return new ParsedField<>(merchant, confidence, confidence < CONFIDENCE_THRESHOLD);
        }
        return ParsedField.missing();
    }

    private ParsedField<LocalDate> dateField(LocalDate date, String token, List<OcrWord> words, double fallbackConfidence) {
        int confidence = confidenceForToken(token, words, fallbackConfidence);
        return new ParsedField<>(date, confidence, confidence < CONFIDENCE_THRESHOLD);
    }

    private int confidenceForToken(String token, List<OcrWord> words, double fallbackConfidence) {
        if (token == null || token.isBlank()) return clamp(fallbackConfidence);
        String needle = token.toLowerCase(Locale.ROOT).replace(",", "");
        double sum = 0;
        int count = 0;
        for (OcrWord word : words) {
            String candidate = word.text().toLowerCase(Locale.ROOT).replace(",", "");
            if (candidate.isBlank()) continue;
            if (candidate.contains(needle) || needle.contains(candidate)) {
                sum += word.confidence();
                count++;
            }
        }
        return count == 0 ? clamp(fallbackConfidence) : clamp(sum / count);
    }

    private int clamp(double value) {
        return Math.max(0, Math.min(100, (int) Math.round(value)));
    }

    private String firstWord(String line) {
        String[] parts = line.trim().split("\\s+");
        return parts.length == 0 ? line : parts[0];
    }

    private int toInt(String value) {
        try {
            return Integer.parseInt(value.trim());
        } catch (NumberFormatException exception) {
            return -1;
        }
    }

    private int normalizeYear(int twoDigit) {
        if (twoDigit < 0) return twoDigit;
        return twoDigit >= 100 ? twoDigit : 2000 + twoDigit;
    }

    private LocalDate build(int day, int month, int year) {
        if (day < 1 || day > 31 || month < 1 || month > 12 || year < 2000 || year > 2100) return null;
        try {
            return LocalDate.of(year, month, day);
        } catch (Exception exception) {
            return null;
        }
    }

    private int monthFromName(String name) {
        String key = name.toLowerCase(Locale.ROOT);
        String[] months = {"jan", "feb", "mar", "apr", "may", "jun", "jul", "aug", "sep", "oct", "nov", "dec"};
        for (int index = 0; index < months.length; index++) {
            if (key.startsWith(months[index])) return index + 1;
        }
        return 0;
    }
}
