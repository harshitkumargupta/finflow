package com.finflow.dto;

import java.math.BigDecimal;

public record CategoryFigure(String category, BigDecimal amount, BigDecimal percentage) {
}
