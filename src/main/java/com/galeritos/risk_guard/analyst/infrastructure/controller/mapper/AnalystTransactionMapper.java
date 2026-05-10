package com.galeritos.risk_guard.analyst.infrastructure.controller.mapper;

import com.galeritos.risk_guard.analyst.application.usecase.dto.TransactionDetailAggregate;
import com.galeritos.risk_guard.analyst.infrastructure.controller.dto.AnalystDecisionHistoryItemResponse;
import com.galeritos.risk_guard.analyst.infrastructure.controller.dto.AnalystReceiverProfileResponse;
import com.galeritos.risk_guard.analyst.infrastructure.controller.dto.AnalystRiskSummaryResponse;
import com.galeritos.risk_guard.analyst.infrastructure.controller.dto.AnalystSenderProfileResponse;
import com.galeritos.risk_guard.analyst.infrastructure.controller.dto.AnalystTransactionDetailResponse;
import com.galeritos.risk_guard.analyst.infrastructure.controller.dto.AnalystTransactionSummaryResponse;
import com.galeritos.risk_guard.banking.domain.model.Transaction;
import com.galeritos.risk_guard.banking.domain.model.TransactionDecisionHistory;
import com.galeritos.risk_guard.identity.domain.model.User;
import com.galeritos.risk_guard.risk.domain.model.RiskAnalysis;

public final class AnalystTransactionMapper {

    private AnalystTransactionMapper() {
    }

    public static AnalystTransactionSummaryResponse toSummary(Transaction t) {
        return new AnalystTransactionSummaryResponse(
                t.getId(),
                t.getSenderId(),
                t.getReceiverId(),
                t.getAmount(),
                t.getStatus().name(),
                t.getRiskLevel() != null ? t.getRiskLevel().name() : null,
                t.getCreatedAt(),
                t.getCustomerDecisionDeadlineAt());
    }

    public static AnalystTransactionDetailResponse toDetail(TransactionDetailAggregate agg) {
        return new AnalystTransactionDetailResponse(
                agg.transaction().getId(),
                agg.transaction().getAmount(),
                agg.transaction().getStatus().name(),
                agg.transaction().getFinancialStatus() != null ? agg.transaction().getFinancialStatus().name() : null,
                agg.transaction().getCreatedAt(),
                agg.transaction().getCustomerDecisionDeadlineAt(),
                toSenderProfile(agg.sender()),
                toReceiverProfile(agg.receiver()),
                agg.riskAnalysis() != null ? toRiskSummary(agg.riskAnalysis()) : null,
                agg.decisionHistory().stream().map(AnalystTransactionMapper::toHistoryItem).toList());
    }

    private static AnalystSenderProfileResponse toSenderProfile(User user) {
        return new AnalystSenderProfileResponse(
                user.getId(),
                user.getName(),
                user.getEmail(),
                user.getStatus().name(),
                user.isSuspect(),
                user.getCreatedAt());
    }

    private static AnalystReceiverProfileResponse toReceiverProfile(User user) {
        return new AnalystReceiverProfileResponse(
                user.getId(),
                user.getName(),
                user.getEmail(),
                user.getStatus().name(),
                user.isSuspect(),
                user.getCreatedAt());
    }

    private static AnalystRiskSummaryResponse toRiskSummary(RiskAnalysis risk) {
        return new AnalystRiskSummaryResponse(
                risk.getScore(),
                risk.getRiskLevel().name(),
                risk.getExplanation(),
                risk.getModelVersion());
    }

    private static AnalystDecisionHistoryItemResponse toHistoryItem(TransactionDecisionHistory h) {
        return new AnalystDecisionHistoryItemResponse(
                h.getAnalystId(),
                h.getDecision().name(),
                h.getFromStatus().name(),
                h.getToStatus().name(),
                h.getReason(),
                h.getCreatedAt());
    }
}
