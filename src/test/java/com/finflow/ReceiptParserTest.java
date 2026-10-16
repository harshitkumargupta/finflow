package com.finflow;

import com.finflow.receipt.OcrResult;
import com.finflow.receipt.OcrWord;
import com.finflow.receipt.ParsedReceipt;
import com.finflow.service.ReceiptParser;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class ReceiptParserTest {

    private final ReceiptParser parser = new ReceiptParser();

    private OcrResult ocr(String text, List<OcrWord> words) {
        double mean = words.isEmpty() ? 0 : words.stream().mapToDouble(OcrWord::confidence).average().orElse(0);
        return new OcrResult(text, words, mean);
    }

    @Test
    void extractsMerchantTotalAndDateFromTypicalReceipt() {
        String text = "CAFE COFFEE DAY\n12/01/2024  10:45\nLatte 199.00\nMuffin 150.00\nSubtotal 349.00\nGRAND TOTAL ₹349.00\nThank you";
        ParsedReceipt result = parser.parse(ocr(text, List.of(
                new OcrWord("CAFE", 95), new OcrWord("COFFEE", 94), new OcrWord("DAY", 93),
                new OcrWord("12/01/2024", 88), new OcrWord("349.00", 92))));

        assertEquals(0, new BigDecimal("349.00").compareTo((BigDecimal) result.total().value()));
        assertFalse(result.total().flagged());
        assertEquals(LocalDate.of(2024, 1, 12), result.date().value());
        assertTrue(((String) result.merchant().value()).toUpperCase().contains("CAFE"));
    }

    @Test
    void swapsDayAndMonthWhenFirstGroupIsMonth() {
        String text = "STORE\n01/13/2024\nTOTAL 50.00";
        ParsedReceipt result = parser.parse(ocr(text, List.of(new OcrWord("01/13/2024", 80), new OcrWord("50.00", 80))));
        assertEquals(LocalDate.of(2024, 1, 13), result.date().value());
    }

    @Test
    void parsesSpelledMonthDate() {
        String text = "SHOP NAME\nDate: 5 Mar 2023\nAmount Payable Rs 1,250.50";
        ParsedReceipt result = parser.parse(ocr(text, List.of(new OcrWord("5", 80), new OcrWord("Mar", 80), new OcrWord("1,250.50", 85))));
        assertEquals(LocalDate.of(2023, 3, 5), result.date().value());
        assertEquals(0, new BigDecimal("1250.50").compareTo((BigDecimal) result.total().value()));
    }

    @Test
    void flagsTotalWhenNoTotalKeywordPresent() {
        String text = "RANDOM MART\nitem one 20.00\nitem two 35.00";
        ParsedReceipt result = parser.parse(ocr(text, List.of(new OcrWord("35.00", 70))));
        assertEquals(0, new BigDecimal("35.00").compareTo((BigDecimal) result.total().value()));
        assertTrue(result.total().flagged());
    }

    @Test
    void ignoresLongReferenceNumbersOnPaymentScreenshots() {
        String text = "Paytm\nUPI Ref No 625335111700\nPaid ₹500.00 to Blue Store\nMobile 9876543210";
        ParsedReceipt result = parser.parse(ocr(text, List.of(new OcrWord("500.00", 90), new OcrWord("625335111700", 95))));
        assertEquals(0, new BigDecimal("500.00").compareTo((BigDecimal) result.total().value()));
    }

    @Test
    void readsAmountOnLineAfterKeywordAndIgnoresMaskedCard() {
        String text = "Payment Successful\nAmount Paid\n1,250.00\nBank HDFC ****4321";
        ParsedReceipt result = parser.parse(ocr(text, List.of(new OcrWord("1,250.00", 90), new OcrWord("4321", 88))));
        assertEquals(0, new BigDecimal("1250.00").compareTo((BigDecimal) result.total().value()));
        assertFalse(result.total().flagged());
    }

    @Test
    void parsesPaytmScreenshotAmountUpiAndPayeeIgnoringAds() {
        String text = "4:18\nPaytm\nVishal Kumar\nUPI ID: gpay-11263393822@okbizaxis\n₹1,000\n"
                + "10 Sep, 4:18 PM Ref No: 625335111700\nSmart tags Smarter budgets\nFood ₹12,400\nWeekly Grocery ₹26,200\nCommute ₹12,400";
        ParsedReceipt result = parser.parse(ocr(text, List.of(
                new OcrWord("Vishal", 95), new OcrWord("Kumar", 95),
                new OcrWord("gpay-11263393822@okbizaxis", 90), new OcrWord("1,000", 92))));

        assertEquals(0, new BigDecimal("1000.00").compareTo((BigDecimal) result.total().value()));
        assertEquals("gpay-11263393822@okbizaxis", result.upiId().value());
        assertTrue(((String) result.merchant().value()).contains("Vishal"));
    }

    @Test
    void reportsMissingFieldsWhenNothingMatches() {
        ParsedReceipt result = parser.parse(ocr("\n\n", List.of()));
        assertNull(result.total().value());
        assertTrue(result.total().flagged());
        assertNull(result.date().value());
        assertNull(result.merchant().value());
    }
}
