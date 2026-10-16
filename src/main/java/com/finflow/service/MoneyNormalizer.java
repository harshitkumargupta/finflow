package com.finflow.service;

import com.finflow.exception.InvalidTransferException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.math.RoundingMode;

@Component
public class MoneyNormalizer {
    private final BigDecimal maximum;

    public MoneyNormalizer(@Value("${finflow.money.maximum:1000000000.00}") BigDecimal maximum) {
        this.maximum = maximum.setScale(2, RoundingMode.UNNECESSARY);
    }

    public BigDecimal normalizePositive(BigDecimal amount) {
        if (amount == null || amount.signum() <= 0 || amount.scale() > 2 || amount.compareTo(maximum) > 0) {
            throw new InvalidTransferException("Amount must be positive, use at most 2 decimal places, and stay within the configured maximum");
        }
        return amount.setScale(2, RoundingMode.HALF_UP);
    }
}
