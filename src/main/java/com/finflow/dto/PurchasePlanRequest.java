package com.finflow.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.math.BigDecimal;

public record PurchasePlanRequest(@NotNull @DecimalMin("0.01") BigDecimal amount,
                                  @Size(max = 120) String description,
                                  @Size(max = 30) String category) { }
