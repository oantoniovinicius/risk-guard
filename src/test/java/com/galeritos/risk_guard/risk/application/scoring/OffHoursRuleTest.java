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

class OffHoursRuleTest {

    private final OffHoursRule rule = new OffHoursRule();

    private RiskScoringContext ctx(int hour) {
        LocalDateTime time = LocalDateTime.now().withHour(hour).withMinute(30);
        return new RiskScoringContext(UUID.randomUUID(), BigDecimal.TEN, time,
                1L, 3L, false, BigDecimal.TEN, false, 0L, false, 0L);
    }

    @Test
    void shouldReturnMaxContributionForLateNightBefore6am() {
        RuleResult result = rule.evaluate(ctx(3));
        assertTrue(result.fired());
        assertEquals(0.20, result.contribution(), 0.001);
        assertTrue(result.signal().contains("late night"));
    }

    @Test
    void shouldReturnHighContributionAfter10pm() {
        RuleResult result = rule.evaluate(ctx(23));
        assertTrue(result.fired());
        assertEquals(0.15, result.contribution(), 0.001);
        assertTrue(result.signal().contains("late night"));
    }

    @Test
    void shouldReturnLowContributionForEarlyMorning() {
        RuleResult result = rule.evaluate(ctx(7));
        assertTrue(result.fired());
        assertEquals(0.08, result.contribution(), 0.001);
        assertTrue(result.signal().contains("off-peak"));
    }

    @Test
    void shouldReturnLowContributionForEarlyEvening() {
        RuleResult result = rule.evaluate(ctx(21));
        assertTrue(result.fired());
        assertEquals(0.08, result.contribution(), 0.001);
    }

    @Test
    void shouldNotFireDuringBusinessHours() {
        assertFalse(rule.evaluate(ctx(10)).fired());
        assertFalse(rule.evaluate(ctx(14)).fired());
        assertFalse(rule.evaluate(ctx(17)).fired());
    }
}
