package com.galeritos.risk_guard.analyst.infrastructure.controller.dto;

import java.math.BigDecimal;

public record AnalystRiskSummaryResponse(
        BigDecimal score,
        String riskLevel,
        String explanation,
        String modelVersion) {
}
