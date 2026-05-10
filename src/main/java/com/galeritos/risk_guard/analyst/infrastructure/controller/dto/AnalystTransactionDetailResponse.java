package com.galeritos.risk_guard.analyst.infrastructure.controller.dto;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

public record AnalystTransactionDetailResponse(
        UUID transactionId,
        BigDecimal amount,
        String status,
        String financialStatus,
        LocalDateTime createdAt,
        LocalDateTime customerDecisionDeadlineAt,
        AnalystSenderProfileResponse sender,
        AnalystReceiverProfileResponse receiver,
        AnalystRiskSummaryResponse riskAnalysis,
        List<AnalystDecisionHistoryItemResponse> decisionHistory) {
}
