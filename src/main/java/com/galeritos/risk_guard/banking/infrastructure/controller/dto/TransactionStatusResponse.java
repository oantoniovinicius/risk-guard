package com.galeritos.risk_guard.banking.infrastructure.controller.dto;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

public record TransactionStatusResponse(
        UUID transactionId,
        String status,
        String financialStatus,
        String riskLevel,
        BigDecimal amount,
        UUID senderId,
        UUID receiverId,
        LocalDateTime customerDecisionDeadlineAt,
        LocalDateTime createdAt) {
}
