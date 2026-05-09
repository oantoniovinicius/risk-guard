package com.galeritos.risk_guard.banking.domain.exception;

import com.galeritos.risk_guard.banking.domain.model.enums.TransactionStatus;

public class InvalidDisputeStateException extends RuntimeException {
    public InvalidDisputeStateException(TransactionStatus currentStatus) {
        super("Cannot open a dispute for a transaction in status " + currentStatus + ". Only APPROVED transactions can be disputed.");
    }
}
