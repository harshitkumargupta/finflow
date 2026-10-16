package com.finflow.dto;

import java.util.List;

public record ReconciliationPageResponse(List<ReconciliationItemResponse> items, int page, int size,
                                         long totalElements, int totalPages) { }
