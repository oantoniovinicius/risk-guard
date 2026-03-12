package com.galeritos.risk_guard.banking.application.event;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

import com.galeritos.risk_guard.banking.domain.model.Transaction;
import com.galeritos.risk_guard.banking.domain.model.enums.FinancialStatus;
import com.galeritos.risk_guard.banking.domain.model.enums.TransactionStatus;

public record TransactionCreatedEvent(
        UUID transactionId,
        UUID senderId,
        UUID receiverId,
        BigDecimal amount,
        TransactionStatus status,
        FinancialStatus financialStatus,
        LocalDateTime createdAt) {

    public static TransactionCreatedEvent from(Transaction transaction) {
        return new TransactionCreatedEvent(
                transaction.getId(),
                transaction.getSenderId(),
                transaction.getReceiverId(),
                transaction.getAmount(),
                transaction.getStatus(),
                transaction.getFinancialStatus(),
                transaction.getCreatedAt());
    }
}
