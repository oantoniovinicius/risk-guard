package com.galeritos.risk_guard.banking.application.event;

import java.math.BigDecimal;
import java.util.UUID;

import com.galeritos.risk_guard.banking.domain.model.Transaction;

public record TransactionStatusChangedEvent(
        UUID eventId,
        String eventType,
        UUID aggregateId,
        String status,
        String financialStatus,
        BigDecimal amount,
        UUID senderId,
        UUID receiverId) {

    public static TransactionStatusChangedEvent from(Transaction transaction, String eventType) {
        return new TransactionStatusChangedEvent(
                UUID.randomUUID(),
                eventType,
                transaction.getId(),
                transaction.getStatus().name(),
                transaction.getFinancialStatus().name(),
                transaction.getAmount(),
                transaction.getSenderId(),
                transaction.getReceiverId());
    }
}
