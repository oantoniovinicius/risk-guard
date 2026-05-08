package com.galeritos.risk_guard.risk.application.scoring;

import org.springframework.stereotype.Component;

import com.galeritos.risk_guard.risk.domain.scoring.RiskRule;
import com.galeritos.risk_guard.risk.domain.scoring.RiskScoringContext;
import com.galeritos.risk_guard.risk.domain.scoring.RuleResult;

@Component
public class ReceiverProfileRule implements RiskRule {

    @Override
    public RuleResult evaluate(RiskScoringContext ctx) {
        if (ctx.receiverFraudConfirmedCount() > 0) {
            return new RuleResult(0.25,
                    String.format("recipient account has %d confirmed fraud case(s) as receiver",
                            ctx.receiverFraudConfirmedCount()));
        }
        if (ctx.receiverIsSuspect()) {
            return new RuleResult(0.15, "recipient account is flagged as suspect");
        }
        return RuleResult.none();
    }
}
