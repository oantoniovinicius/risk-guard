package com.galeritos.risk_guard.risk.application.scoring;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

import org.junit.jupiter.api.Test;

import com.galeritos.risk_guard.risk.domain.scoring.RiskScoringContext;
import com.galeritos.risk_guard.risk.domain.scoring.RuleResult;

class AmountDeviationRuleTest {

    private final AmountDeviationRule rule = new AmountDeviationRule();

    private RiskScoringContext ctx(BigDecimal amount, BigDecimal avg) {
        return new RiskScoringContext(UUID.randomUUID(), amount, LocalDateTime.now(),
                1L, 3L, false, avg, false, 0L, false, 0L);
    }

    @Test
    void shouldReturnBaselineSignalWhenNoHistory() {
        RuleResult result = rule.evaluate(ctx(new BigDecimal("500.00"), null));
        assertTrue(result.fired());
        assertEquals(0.15, result.contribution(), 0.001);
        assertTrue(result.signal().contains("no prior transaction history"));
    }

    @Test
    void shouldReturnBaselineSignalWhenAvgIsZero() {
        RuleResult result = rule.evaluate(ctx(new BigDecimal("500.00"), BigDecimal.ZERO));
        assertTrue(result.fired());
        assertEquals(0.15, result.contribution(), 0.001);
    }

    @Test
    void shouldNotFireWhenRatioBelow1_5() {
        RuleResult result = rule.evaluate(ctx(new BigDecimal("140.00"), new BigDecimal("100.00")));
        assertFalse(result.fired());
    }

    @Test
    void shouldReturnLowContributionForRatioBetween1_5And2() {
        RuleResult result = rule.evaluate(ctx(new BigDecimal("170.00"), new BigDecimal("100.00")));
        assertTrue(result.fired());
        assertEquals(0.10, result.contribution(), 0.001);
        assertTrue(result.signal().contains("R$170.00"));
        assertTrue(result.signal().contains("1.7×"));
    }

    @Test
    void shouldReturnMediumContributionForRatioBetween2And3() {
        RuleResult result = rule.evaluate(ctx(new BigDecimal("250.00"), new BigDecimal("100.00")));
        assertEquals(0.20, result.contribution(), 0.001);
    }

    @Test
    void shouldReturnHighContributionForRatioBetween3And5() {
        RuleResult result = rule.evaluate(ctx(new BigDecimal("400.00"), new BigDecimal("100.00")));
        assertEquals(0.30, result.contribution(), 0.001);
    }

    @Test
    void shouldReturnMaxContributionForRatioAbove5() {
        RuleResult result = rule.evaluate(ctx(new BigDecimal("600.00"), new BigDecimal("100.00")));
        assertEquals(0.40, result.contribution(), 0.001);
        assertTrue(result.signal().contains("6.0×"));
    }
}
