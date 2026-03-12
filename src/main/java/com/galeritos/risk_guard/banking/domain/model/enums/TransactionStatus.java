package com.galeritos.risk_guard.banking.domain.model.enums;

public enum TransactionStatus {
    CREATED,
    ANALYZING,
    AWAITING_CUSTOMER,
    AWAITING_ANALYST,
    APPROVED,
    DENIED,
    FRAUD_CONFIRMED
}
