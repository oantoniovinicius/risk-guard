package com.galeritos.risk_guard.admin.infrastructure.controller.dto;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

public record AdminTransactionResponse(
        UUID id,
        UUID senderId,
        UUID receiverId,
        BigDecimal amount,
        String status,
        String financialStatus,
        String riskLevel,
        LocalDateTime customerDecisionDeadlineAt,
        LocalDateTime createdAt) {
}
