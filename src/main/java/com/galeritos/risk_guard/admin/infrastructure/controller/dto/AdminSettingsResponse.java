package com.galeritos.risk_guard.admin.infrastructure.controller.dto;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.LocalTime;

public record AdminSettingsResponse(
        BigDecimal mediumRiskThreshold,
        BigDecimal highRiskThreshold,
        LocalTime businessStartTime,
        LocalTime businessEndTime,
        LocalDateTime updatedAt) {
}
