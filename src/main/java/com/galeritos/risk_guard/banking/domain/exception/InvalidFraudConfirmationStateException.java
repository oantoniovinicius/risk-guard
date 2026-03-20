package com.galeritos.risk_guard.banking.domain.exception;

import com.galeritos.risk_guard.banking.domain.model.enums.TransactionStatus;

public class InvalidFraudConfirmationStateException extends RuntimeException {
    public InvalidFraudConfirmationStateException(TransactionStatus currentStatus) {
        super("Transaction is not FRAUD_CONFIRMED. Current status: " + currentStatus);
    }
}
