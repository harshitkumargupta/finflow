package com.finflow;

import com.finflow.service.FinancialIntelligenceService;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.assertEquals;

class FinancialIntelligenceRulesTest {
    @Test
    void purchaseRulesCoverSafeCautionAndNotRecommended() {
        assertEquals("SAFE", FinancialIntelligenceService.recommendPurchase(
                new BigDecimal("1000"), new BigDecimal("800"), new BigDecimal("100")));
        assertEquals("CAUTION", FinancialIntelligenceService.recommendPurchase(
                new BigDecimal("1000"), new BigDecimal("200"), new BigDecimal("100")));
        assertEquals("CAUTION", FinancialIntelligenceService.recommendPurchase(
                new BigDecimal("1000"), new BigDecimal("800"), new BigDecimal("-10")));
        assertEquals("NOT_RECOMMENDED", FinancialIntelligenceService.recommendPurchase(
                new BigDecimal("1000"), new BigDecimal("-1"), null));
    }
}
