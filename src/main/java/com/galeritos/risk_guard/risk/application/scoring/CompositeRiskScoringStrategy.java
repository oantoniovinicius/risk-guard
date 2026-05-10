package com.galeritos.risk_guard.risk.application.scoring;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.List;

import org.springframework.stereotype.Service;

import com.galeritos.risk_guard.risk.domain.scoring.RiskRule;
import com.galeritos.risk_guard.risk.domain.scoring.RiskScore;
import com.galeritos.risk_guard.risk.domain.scoring.RiskScoringContext;
import com.galeritos.risk_guard.risk.domain.scoring.RiskScoringStrategy;
import com.galeritos.risk_guard.risk.domain.scoring.RuleResult;

@Service
public class CompositeRiskScoringStrategy implements RiskScoringStrategy {

    private final List<RiskRule> rules;

    public CompositeRiskScoringStrategy(List<RiskRule> rules) {
        this.rules = rules;
    }

    @Override
    public RiskScore score(RiskScoringContext context) {
        double total = 0.0;
        List<String> signals = new ArrayList<>();

        for (RiskRule rule : rules) {
            RuleResult result = rule.evaluate(context);
            if (result.fired()) {
                total += result.contribution();
                signals.add(String.format("%s (+%.2f)", result.signal(), result.contribution()));
            }
        }

        BigDecimal score = BigDecimal.valueOf(Math.min(1.0, total))
                .setScale(4, RoundingMode.HALF_UP);

        String explanation = signals.isEmpty()
                ? "no significant risk signals detected"
                : String.join("; ", signals);

        return new RiskScore(score, explanation);
    }
}
