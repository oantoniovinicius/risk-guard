package com.galeritos.risk_guard.config;

import java.math.BigDecimal;
import java.time.LocalTime;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "app.admin.defaults")
public record AdminSettingsDefaultsProperties(
        BigDecimal mediumRiskThreshold,
        BigDecimal highRiskThreshold,
        LocalTime businessStartTime,
        LocalTime businessEndTime) {
}
