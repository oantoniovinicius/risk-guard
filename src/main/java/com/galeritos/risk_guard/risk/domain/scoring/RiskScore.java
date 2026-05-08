package com.galeritos.risk_guard.risk.domain.scoring;

import java.math.BigDecimal;

public record RiskScore(BigDecimal score, String explanation) {
}
