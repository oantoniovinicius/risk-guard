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

class NewReceiverRuleTest {

    private final NewReceiverRule rule = new NewReceiverRule();

    private RiskScoringContext ctx(boolean firstTransaction) {
        return new RiskScoringContext(UUID.randomUUID(), BigDecimal.TEN, LocalDateTime.now(),
                1L, 3L, firstTransaction, BigDecimal.TEN, false, 0L, false, 0L);
    }

    @Test
    void shouldFireWhenFirstTransactionWithReceiver() {
        RuleResult result = rule.evaluate(ctx(true));
        assertTrue(result.fired());
        assertEquals(0.10, result.contribution(), 0.001);
        assertTrue(result.signal().contains("first transaction with this recipient"));
    }

    @Test
    void shouldNotFireWhenReceiverIsKnown() {
        RuleResult result = rule.evaluate(ctx(false));
        assertFalse(result.fired());
    }
}
