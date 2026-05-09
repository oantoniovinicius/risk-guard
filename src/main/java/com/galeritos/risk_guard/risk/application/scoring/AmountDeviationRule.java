package com.galeritos.risk_guard.risk.application.scoring;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Locale;

import org.springframework.stereotype.Component;

import com.galeritos.risk_guard.risk.domain.scoring.RiskRule;
import com.galeritos.risk_guard.risk.domain.scoring.RiskScoringContext;
import com.galeritos.risk_guard.risk.domain.scoring.RuleResult;

@Component
public class AmountDeviationRule implements RiskRule {

    @Override
    public RuleResult evaluate(RiskScoringContext ctx) {
        BigDecimal avg = ctx.senderAvgAmountLast30Days();

        if (avg == null || avg.compareTo(BigDecimal.ZERO) == 0) {
            return new RuleResult(0.15, "no prior transaction history to establish an amount baseline");
        }

        double ratio = ctx.amount().divide(avg, 4, RoundingMode.HALF_UP).doubleValue();

        if (ratio < 1.5) {
            return RuleResult.none();
        }

        String signal = String.format(Locale.US,
                "amount R$%.2f is %.1f× above 30-day avg (R$%.2f)",
                ctx.amount(), ratio, avg);

        if (ratio >= 5.0) return new RuleResult(0.40, signal);
        if (ratio >= 3.0) return new RuleResult(0.30, signal);
        if (ratio >= 2.0) return new RuleResult(0.20, signal);
        return new RuleResult(0.10, signal);
    }
}
