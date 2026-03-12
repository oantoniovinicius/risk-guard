package com.galeritos.risk_guard.banking.infrastructure.controller.dto;

import java.math.BigDecimal;
import java.util.UUID;

public record CreateTransferRequest(UUID senderId, UUID receiverId, BigDecimal amount) {

}
