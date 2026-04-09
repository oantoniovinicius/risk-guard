package com.galeritos.risk_guard.config;

import java.math.BigDecimal;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "app.banking")
public record BankingProperties(BigDecimal initialBalance) {
}
