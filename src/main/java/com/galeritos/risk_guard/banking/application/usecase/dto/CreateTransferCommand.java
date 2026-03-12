package com.galeritos.risk_guard.banking.application.usecase.dto;

import java.math.BigDecimal;
import java.util.UUID;

public record CreateTransferCommand(UUID senderId, UUID receiverId, BigDecimal amount) {
}
