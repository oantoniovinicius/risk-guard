package com.galeritos.risk_guard.risk.application.port.out;

import java.math.BigDecimal;

public interface RiskThresholdsProvider {
    BigDecimal mediumRiskThreshold();
    BigDecimal highRiskThreshold();
}
