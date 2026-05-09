package com.galeritos.risk_guard.risk.application.scoring;

import org.springframework.stereotype.Component;

import com.galeritos.risk_guard.risk.domain.scoring.RiskRule;
import com.galeritos.risk_guard.risk.domain.scoring.RiskScoringContext;
import com.galeritos.risk_guard.risk.domain.scoring.RuleResult;

@Component
public class UserHistoryRule implements RiskRule {

    @Override
    public RuleResult evaluate(RiskScoringContext ctx) {
        if (ctx.senderFraudConfirmedCount() > 0) {
            return new RuleResult(0.15,
                    String.format("sender has %d confirmed fraud case(s)", ctx.senderFraudConfirmedCount()));
        }
        if (ctx.senderIsSuspect()) {
            return new RuleResult(0.10, "sender is flagged as suspect");
        }
        return RuleResult.none();
    }
}
