package com.galeritos.risk_guard.admin.infrastructure.controller.dto;

import java.math.BigDecimal;

public record UpdateAdminSettingsRequest(
        BigDecimal mediumRiskThreshold,
        BigDecimal highRiskThreshold) {
}
