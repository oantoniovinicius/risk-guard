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

class TransactionFrequencyRuleTest {

    private final TransactionFrequencyRule rule = new TransactionFrequencyRule();

    private RiskScoringContext ctx(long count1h, long count24h) {
        return new RiskScoringContext(UUID.randomUUID(), BigDecimal.TEN, LocalDateTime.now(),
                count1h, count24h, false, BigDecimal.TEN, false, 0L, false, 0L);
    }

    @Test
    void shouldNotFireForLowVolume() {
        assertFalse(rule.evaluate(ctx(2L, 4L)).fired());
    }

    @Test
    void shouldFireForModerateHourlyVolume() {
        RuleResult result = rule.evaluate(ctx(3L, 2L));
        assertTrue(result.fired());
        assertEquals(0.10, result.contribution(), 0.001);
        assertTrue(result.signal().contains("3 transactions in the last hour"));
    }

    @Test
    void shouldFireForHighHourlyVolume() {
        RuleResult result = rule.evaluate(ctx(6L, 2L));
        assertTrue(result.fired());
        assertEquals(0.15, result.contribution(), 0.001);
    }

    @Test
    void shouldFireForHigh24hVolume() {
        RuleResult result = rule.evaluate(ctx(1L, 10L));
        assertTrue(result.fired());
        assertEquals(0.10, result.contribution(), 0.001);
        assertTrue(result.signal().contains("10 transactions in the last 24 hours"));
    }

    @Test
    void shouldCombineBothSignalsAndCapAt0_25() {
        RuleResult result = rule.evaluate(ctx(6L, 10L));
        assertTrue(result.fired());
        assertEquals(0.25, result.contribution(), 0.001);
        assertTrue(result.signal().contains("last hour"));
        assertTrue(result.signal().contains("last 24 hours"));
    }
}
