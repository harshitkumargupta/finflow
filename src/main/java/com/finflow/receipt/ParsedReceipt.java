package com.finflow.receipt;

import java.math.BigDecimal;
import java.time.LocalDate;

public record ParsedReceipt(ParsedField<String> merchant, ParsedField<LocalDate> date, ParsedField<BigDecimal> total,
                            ParsedField<String> upiId) {
}
