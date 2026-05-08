package com.galeritos.risk_guard.risk.domain.scoring;

public interface RiskRule {
    RuleResult evaluate(RiskScoringContext context);
}
