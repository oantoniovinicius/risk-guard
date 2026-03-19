package com.galeritos.risk_guard.banking.domain.exception;

import com.galeritos.risk_guard.banking.domain.model.enums.TransactionStatus;

public class InvalidTransactionStateTransitionException extends RuntimeException {
    public InvalidTransactionStateTransitionException(TransactionStatus from, TransactionStatus to) {
        super("Invalid transaction status transition: " + from + " -> " + to);
    }
}
