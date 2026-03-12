package com.galeritos.risk_guard.banking.domain.exception;

public class AccountNotFoundException extends RuntimeException {
    private final String message;

    public AccountNotFoundException(String message) {
        this.message = message;
    }

    public String getMessage() {
        return message;
    }
}
