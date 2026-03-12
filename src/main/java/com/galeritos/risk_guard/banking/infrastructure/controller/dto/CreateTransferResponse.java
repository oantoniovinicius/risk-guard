package com.galeritos.risk_guard.banking.infrastructure.controller.dto;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

public record CreateTransferResponse(
        UUID transactionId,
        UUID senderId,
        UUID receiverId,
        BigDecimal amount,
        String status,
        LocalDateTime createdAt) {
}