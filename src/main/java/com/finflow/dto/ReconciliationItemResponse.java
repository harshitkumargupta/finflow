package com.finflow.dto;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public record ReconciliationItemResponse(Long paymentId, String referenceNumber, BigDecimal internalAmount,
                                         String status, String reconciliationResult, String notes,
                                         LocalDateTime timestamp) { }
