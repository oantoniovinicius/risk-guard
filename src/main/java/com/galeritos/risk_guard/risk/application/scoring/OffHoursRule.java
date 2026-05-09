package com.galeritos.risk_guard.risk.application.scoring;

import java.time.format.DateTimeFormatter;

import org.springframework.stereotype.Component;

import com.galeritos.risk_guard.risk.domain.scoring.RiskRule;
import com.galeritos.risk_guard.risk.domain.scoring.RiskScoringContext;
import com.galeritos.risk_guard.risk.domain.scoring.RuleResult;

@Component
public class OffHoursRule implements RiskRule {

    private static final DateTimeFormatter TIME_FORMAT = DateTimeFormatter.ofPattern("HH:mm");

    @Override
    public RuleResult evaluate(RiskScoringContext ctx) {
        int hour = ctx.initiatedAt().getHour();
        String time = ctx.initiatedAt().format(TIME_FORMAT);

        if (hour < 6) {
            return new RuleResult(0.20, String.format("initiated at %s (late night)", time));
        }
        if (hour >= 22) {
            return new RuleResult(0.15, String.format("initiated at %s (late night)", time));
        }
        if (hour < 8 || hour >= 20) {
            return new RuleResult(0.08, String.format("initiated at %s (off-peak hours)", time));
        }
        return RuleResult.none();
    }
}
