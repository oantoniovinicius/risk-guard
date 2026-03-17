package com.galeritos.risk_guard.risk.application.event;

import java.math.BigDecimal;
import java.util.UUID;

import com.galeritos.risk_guard.risk.domain.model.RiskAnalysis;
import com.galeritos.risk_guard.shared.enums.RiskLevel;
import com.galeritos.risk_guard.shared.events.EventTypes;

public record TransactionAnalyzedEvent(
        UUID eventId,
        String eventType,
        UUID transactionId,
        RiskLevel riskLevel,
        BigDecimal score,
        String explanation) {

    public static TransactionAnalyzedEvent from(RiskAnalysis riskAnalysis) {
        return new TransactionAnalyzedEvent(
                UUID.randomUUID(),
                EventTypes.TRANSACTION_ANALYZED,
                riskAnalysis.getTransactionId(),
                riskAnalysis.getRiskLevel(),
                riskAnalysis.getScore(),
                riskAnalysis.getExplanation());
    }
}
