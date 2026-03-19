package com.galeritos.risk_guard.banking.domain.exception;

import com.galeritos.risk_guard.banking.domain.model.enums.TransactionStatus;

public class InvalidTransactionFinancialFinalizationException extends RuntimeException {
    public InvalidTransactionFinancialFinalizationException(TransactionStatus status) {
        super("Transaction status does not allow financial finalization: " + status);
    }
}
