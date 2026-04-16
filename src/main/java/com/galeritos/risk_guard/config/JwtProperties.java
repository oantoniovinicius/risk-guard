package com.galeritos.risk_guard.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "app.security.jwt")
public record JwtProperties(String secretBase64, long expirationMs, String issuer) {
}
