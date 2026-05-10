package com.galeritos.risk_guard.analyst.application.usecase.dto;

import java.util.List;

import com.galeritos.risk_guard.banking.domain.model.Transaction;
import com.galeritos.risk_guard.banking.domain.model.TransactionDecisionHistory;
import com.galeritos.risk_guard.identity.domain.model.User;
import com.galeritos.risk_guard.risk.domain.model.RiskAnalysis;

public record TransactionDetailAggregate(
        Transaction transaction,
        User sender,
        User receiver,
        RiskAnalysis riskAnalysis,
        List<TransactionDecisionHistory> decisionHistory) {
}
