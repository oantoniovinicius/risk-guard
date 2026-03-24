package com.galeritos.risk_guard.banking.domain.exception;

import com.galeritos.risk_guard.banking.domain.model.enums.CustomerConfirmationDecision;
import com.galeritos.risk_guard.banking.domain.model.enums.TransactionStatus;

public class InvalidCustomerConfirmationStateException extends RuntimeException {
    public InvalidCustomerConfirmationStateException(TransactionStatus currentStatus,
            CustomerConfirmationDecision decision) {
        super("Cannot apply customer decision " + decision + " when transaction is in status " + currentStatus);
    }
}
