package com.galeritos.risk_guard.risk.application.scoring;

import org.springframework.stereotype.Component;

import com.galeritos.risk_guard.risk.domain.scoring.RiskRule;
import com.galeritos.risk_guard.risk.domain.scoring.RiskScoringContext;
import com.galeritos.risk_guard.risk.domain.scoring.RuleResult;

@Component
public class NewReceiverRule implements RiskRule {

    @Override
    public RuleResult evaluate(RiskScoringContext ctx) {
        if (ctx.firstTransactionWithReceiver()) {
            return new RuleResult(0.10, "first transaction with this recipient");
        }
        return RuleResult.none();
    }
}
