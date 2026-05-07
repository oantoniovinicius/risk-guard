package com.galeritos.risk_guard.admin.infrastructure.controller.mapper;

import com.galeritos.risk_guard.admin.infrastructure.controller.dto.AdminTransactionResponse;
import com.galeritos.risk_guard.banking.domain.model.Transaction;

public final class AdminTransactionMapper {

    private AdminTransactionMapper() {
    }

    public static AdminTransactionResponse toResponse(Transaction t) {
        return new AdminTransactionResponse(
                t.getId(),
                t.getSenderId(),
                t.getReceiverId(),
                t.getAmount(),
                t.getStatus().name(),
                t.getFinancialStatus().name(),
                t.getRiskLevel() == null ? null : t.getRiskLevel().name(),
                t.getCustomerDecisionDeadlineAt(),
                t.getCreatedAt());
    }
}
