package com.galeritos.risk_guard.risk.domain.scoring;

public record RuleResult(double contribution, String signal) {

    public static RuleResult none() {
        return new RuleResult(0.0, null);
    }

    public boolean fired() {
        return contribution > 0.0;
    }
}
