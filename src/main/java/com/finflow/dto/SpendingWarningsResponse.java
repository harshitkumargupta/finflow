package com.finflow.dto;

import java.util.List;

public record SpendingWarningsResponse(List<SpendingWarningResponse> warnings, boolean aiGenerated, String source) {
}
