package com.galeritos.risk_guard.config;

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Configuration
@EnableConfigurationProperties(BankingProperties.class)
public class BankingConfig {
}
