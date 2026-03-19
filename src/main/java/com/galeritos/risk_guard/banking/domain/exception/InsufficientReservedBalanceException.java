package com.galeritos.risk_guard.banking.domain.exception;

import java.math.BigDecimal;

public class InsufficientReservedBalanceException extends RuntimeException {
    public InsufficientReservedBalanceException(BigDecimal requested, BigDecimal availableReserved) {
        super("Insufficient reserved balance. Requested: " + requested + ", available: " + availableReserved);
    }
}
