package com.galeritos.risk_guard.banking.domain.exception;

public class InvalidTransferException extends RuntimeException {
    private final String message;

    public InvalidTransferException(String message) {
        this.message = message;
    }

    public String getMessage() {
        return message;
    }
}
