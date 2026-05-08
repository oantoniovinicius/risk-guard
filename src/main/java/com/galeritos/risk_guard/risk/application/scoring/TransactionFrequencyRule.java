package com.galeritos.risk_guard.risk.application.scoring;

import java.util.ArrayList;
import java.util.List;

import org.springframework.stereotype.Component;

import com.galeritos.risk_guard.risk.domain.scoring.RiskRule;
import com.galeritos.risk_guard.risk.domain.scoring.RiskScoringContext;
import com.galeritos.risk_guard.risk.domain.scoring.RuleResult;

@Component
public class TransactionFrequencyRule implements RiskRule {

    @Override
    public RuleResult evaluate(RiskScoringContext ctx) {
        double contribution = 0.0;
        List<String> signals = new ArrayList<>();

        long count1h = ctx.transactionCount1h();
        if (count1h >= 5) {
            contribution += 0.15;
            signals.add(count1h + " transactions in the last hour");
        } else if (count1h >= 3) {
            contribution += 0.10;
            signals.add(count1h + " transactions in the last hour");
        }

        long count24h = ctx.transactionCount24h();
        if (count24h >= 10) {
            contribution += 0.10;
            signals.add(count24h + " transactions in the last 24 hours");
        } else if (count24h >= 5) {
            contribution += 0.05;
            signals.add(count24h + " transactions in the last 24 hours");
        }

        if (signals.isEmpty()) {
            return RuleResult.none();
        }

        return new RuleResult(Math.min(0.25, contribution), String.join("; ", signals));
    }
}
