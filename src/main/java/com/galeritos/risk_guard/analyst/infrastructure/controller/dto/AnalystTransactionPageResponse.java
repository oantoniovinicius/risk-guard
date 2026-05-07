package com.galeritos.risk_guard.analyst.infrastructure.controller.dto;

import java.util.List;

public record AnalystTransactionPageResponse(
        int page,
        int size,
        long totalElements,
        int totalPages,
        List<AnalystTransactionSummaryResponse> content) {
}
