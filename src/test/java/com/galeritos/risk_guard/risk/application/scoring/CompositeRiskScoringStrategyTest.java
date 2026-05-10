package com.galeritos.risk_guard.risk.application.scoring;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

import org.junit.jupiter.api.Test;

import com.galeritos.risk_guard.risk.domain.scoring.RiskScore;
import com.galeritos.risk_guard.risk.domain.scoring.RiskScoringContext;

class CompositeRiskScoringStrategyTest {

    private final CompositeRiskScoringStrategy strategy = new CompositeRiskScoringStrategy(
            List.of(
                    new AmountDeviationRule(),
                    new OffHoursRule(),
                    new TransactionFrequencyRule(),
                    new NewReceiverRule(),
                    new ReceiverProfileRule()));

    private RiskScoringContext ctx(
            BigDecimal amount, BigDecimal avg, int hour,
            long count1h, long count24h,
            boolean firstReceiver, boolean senderSuspect, long senderFraudCount,
            boolean receiverSuspect, long receiverFraudCount) {
        LocalDateTime time = LocalDateTime.now().withHour(hour).withMinute(0);
        return new RiskScoringContext(UUID.randomUUID(), amount, time,
                count1h, count24h, firstReceiver, avg,
                senderSuspect, senderFraudCount,
                receiverSuspect, receiverFraudCount);
    }

    @Test
    void shouldReturnNoSignalExplanationWhenNoRulesFire() {
        RiskScoringContext context = ctx(
                new BigDecimal("100.00"), new BigDecimal("100.00"),
                10, 1L, 2L, false, false, 0L, false, 0L);

        RiskScore score = strategy.score(context);

        assertEquals(0, BigDecimal.ZERO.compareTo(score.score()));
        assertEquals("no significant risk signals detected", score.explanation());
    }

    @Test
    void shouldCombineSignalsFromMultipleRules() {
        RiskScoringContext context = ctx(
                new BigDecimal("600.00"), new BigDecimal("100.00"),
                3, 1L, 2L, true, false, 0L, false, 0L);

        RiskScore score = strategy.score(context);

        assertTrue(score.score().compareTo(BigDecimal.ZERO) > 0);
        assertTrue(score.explanation().contains("6.0×"));
        assertTrue(score.explanation().contains("late night"));
        assertTrue(score.explanation().contains("first transaction with this recipient"));
    }

    @Test
    void shouldCapScoreAt1WhenAllRulesFire() {
        RiskScoringContext context = ctx(
                new BigDecimal("600.00"), new BigDecimal("100.00"),
                3, 6L, 12L, true, false, 2L, false, 3L);

        RiskScore score = strategy.score(context);

        assertEquals(0, new BigDecimal("1.0000").compareTo(score.score()));
    }

    @Test
    void shouldJoinSignalsWithSemicolon() {
        RiskScoringContext context = ctx(
                new BigDecimal("300.00"), new BigDecimal("100.00"),
                3, 1L, 2L, true, false, 0L, false, 0L);

        RiskScore score = strategy.score(context);

        assertTrue(score.explanation().contains(";"));
    }

    @Test
    void shouldFireReceiverProfileWhenReceiverHasFraudHistory() {
        RiskScoringContext context = ctx(
                new BigDecimal("100.00"), new BigDecimal("100.00"),
                10, 1L, 2L, false, false, 0L, false, 2L);

        RiskScore score = strategy.score(context);

        assertTrue(score.score().compareTo(BigDecimal.ZERO) > 0);
        assertTrue(score.explanation().contains("recipient account has 2 confirmed fraud case(s)"));
    }
}
