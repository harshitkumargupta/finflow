package com.finflow;

import com.finflow.service.MoneyNormalizer;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.*;

class MoneyNormalizerTest {
    private final MoneyNormalizer normalizer = new MoneyNormalizer(new BigDecimal("1000.00"));

    @Test
    void rejectsOverPreciseAndOutOfRangeAmounts() {
        assertThrows(RuntimeException.class, () -> normalizer.normalizePositive(new BigDecimal("1.001")));
        assertThrows(RuntimeException.class, () -> normalizer.normalizePositive(new BigDecimal("1000.01")));
        assertEquals(new BigDecimal("10.00"), normalizer.normalizePositive(new BigDecimal("10")));
    }
}
