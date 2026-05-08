package com.galeritos.risk_guard.risk.domain.scoring;

public interface RiskScoringStrategy {
    RiskScore score(RiskScoringContext context);
}
