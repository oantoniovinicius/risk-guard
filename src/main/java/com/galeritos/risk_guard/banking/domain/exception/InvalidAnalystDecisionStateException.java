package com.galeritos.risk_guard.banking.domain.exception;

import com.galeritos.risk_guard.banking.domain.model.enums.AnalystDecision;
import com.galeritos.risk_guard.banking.domain.model.enums.TransactionStatus;

public class InvalidAnalystDecisionStateException extends RuntimeException {
    public InvalidAnalystDecisionStateException(TransactionStatus currentStatus, AnalystDecision decision) {
        super("Cannot apply analyst decision " + decision + " when transaction is in status " + currentStatus);
    }
}
