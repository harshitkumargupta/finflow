package com.finflow.dto;

import java.math.BigDecimal;
import java.time.LocalDate;

public record GoalFeasibility(Long goalId, String name, BigDecimal target,
                              BigDecimal currentAmount, BigDecimal remaining,
                              LocalDate deadline, BigDecimal requiredMonthlyContribution,
                              BigDecimal currentAverageContribution, LocalDate projectedCompletionDate,
                              boolean feasible, String explanation) { }
