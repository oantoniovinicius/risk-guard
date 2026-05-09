package com.galeritos.risk_guard.risk.domain.scoring;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

public record RiskScoringContext(
        UUID transactionId,
        BigDecimal amount,
        LocalDateTime initiatedAt,
        long transactionCount1h,
        long transactionCount24h,
        boolean firstTransactionWithReceiver,
        BigDecimal senderAvgAmountLast30Days,
        boolean senderIsSuspect,
        long senderFraudConfirmedCount,
        boolean receiverIsSuspect,
        long receiverFraudConfirmedCount) {
}
