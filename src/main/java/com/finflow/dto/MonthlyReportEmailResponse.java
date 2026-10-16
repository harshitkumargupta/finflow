package com.finflow.dto;

public record MonthlyReportEmailResponse(String status, boolean delivered, String month, String message) {
}
