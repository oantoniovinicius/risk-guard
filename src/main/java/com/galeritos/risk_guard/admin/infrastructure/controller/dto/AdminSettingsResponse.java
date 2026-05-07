package com.galeritos.risk_guard.admin.infrastructure.controller.dto;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public record AdminSettingsResponse(
        BigDecimal mediumRiskThreshold,
        BigDecimal highRiskThreshold,
        LocalDateTime updatedAt) {
}
