package com.galeritos.risk_guard.banking.domain.exception;

public class InsufficientBalanceException extends RuntimeException {
    private final String message;

    public InsufficientBalanceException(String message) {
        this.message = message;
    }

    public String getMessage() {
        return message;
    }
}
