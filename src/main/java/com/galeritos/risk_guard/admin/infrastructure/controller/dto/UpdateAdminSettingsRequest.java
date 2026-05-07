package com.galeritos.risk_guard.admin.infrastructure.controller.dto;

import java.math.BigDecimal;
import java.time.LocalTime;

public record UpdateAdminSettingsRequest(
        BigDecimal mediumRiskThreshold,
        BigDecimal highRiskThreshold,
        LocalTime businessStartTime,
        LocalTime businessEndTime) {
}
