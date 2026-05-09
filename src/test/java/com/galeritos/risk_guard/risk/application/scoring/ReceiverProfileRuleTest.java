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

class ReceiverProfileRuleTest {

    private final ReceiverProfileRule rule = new ReceiverProfileRule();

    private RiskScoringContext ctx(boolean receiverSuspect, long receiverFraudCount) {
        return new RiskScoringContext(UUID.randomUUID(), BigDecimal.TEN, LocalDateTime.now(),
                1L, 3L, false, BigDecimal.TEN, false, 0L, receiverSuspect, receiverFraudCount);
    }

    @Test
    void shouldNotFireForCleanReceiver() {
        assertFalse(rule.evaluate(ctx(false, 0L)).fired());
    }

    @Test
    void shouldFireForSuspectReceiver() {
        RuleResult result = rule.evaluate(ctx(true, 0L));
        assertTrue(result.fired());
        assertEquals(0.15, result.contribution(), 0.001);
        assertTrue(result.signal().contains("recipient account is flagged as suspect"));
    }

    @Test
    void shouldReturnHigherContributionWhenReceiverHasFraudHistory() {
        RuleResult result = rule.evaluate(ctx(false, 3L));
        assertTrue(result.fired());
        assertEquals(0.25, result.contribution(), 0.001);
        assertTrue(result.signal().contains("3 confirmed fraud case(s) as receiver"));
    }

    @Test
    void shouldPrioritizeFraudHistoryOverSuspectFlag() {
        RuleResult result = rule.evaluate(ctx(true, 2L));
        assertEquals(0.25, result.contribution(), 0.001);
        assertTrue(result.signal().contains("confirmed fraud case(s) as receiver"));
    }
}
