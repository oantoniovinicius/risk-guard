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

class UserHistoryRuleTest {

    private final UserHistoryRule rule = new UserHistoryRule();

    private RiskScoringContext ctx(boolean suspect, long fraudCount) {
        return new RiskScoringContext(UUID.randomUUID(), BigDecimal.TEN, LocalDateTime.now(),
                1L, 3L, false, BigDecimal.TEN, suspect, fraudCount, false, 0L);
    }

    @Test
    void shouldNotFireForCleanUser() {
        assertFalse(rule.evaluate(ctx(false, 0L)).fired());
    }

    @Test
    void shouldFireForSuspectUser() {
        RuleResult result = rule.evaluate(ctx(true, 0L));
        assertTrue(result.fired());
        assertEquals(0.10, result.contribution(), 0.001);
        assertTrue(result.signal().contains("flagged as suspect"));
    }

    @Test
    void shouldReturnHigherContributionForFraudConfirmedHistory() {
        RuleResult result = rule.evaluate(ctx(false, 2L));
        assertTrue(result.fired());
        assertEquals(0.15, result.contribution(), 0.001);
        assertTrue(result.signal().contains("2 confirmed fraud case(s)"));
    }

    @Test
    void shouldPrioritizeFraudConfirmedOverSuspectFlag() {
        RuleResult result = rule.evaluate(ctx(true, 1L));
        assertEquals(0.15, result.contribution(), 0.001);
        assertTrue(result.signal().contains("confirmed fraud"));
    }
}
