package com.galeritos.risk_guard.banking.application.event;

import java.math.BigDecimal;
import java.util.UUID;

import com.galeritos.risk_guard.banking.domain.model.Transaction;
import com.galeritos.risk_guard.shared.events.EventTypes;

public record TransactionCreatedEvent(
        UUID eventId,
        String eventType,
        UUID aggregateId,
        BigDecimal amount,
        UUID senderId,
        UUID receiverId) {

    public static TransactionCreatedEvent from(Transaction transaction) {
        return new TransactionCreatedEvent(
                UUID.randomUUID(),
                EventTypes.TRANSACTION_CREATED,
                transaction.getId(),
                transaction.getAmount(),
                transaction.getSenderId(),
                transaction.getReceiverId());
    }
}
