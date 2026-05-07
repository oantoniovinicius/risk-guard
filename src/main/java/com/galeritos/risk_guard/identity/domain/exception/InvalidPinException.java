package com.galeritos.risk_guard.identity.domain.exception;

public class InvalidPinException extends RuntimeException {
    public InvalidPinException() {
        super("Invalid PIN.");
    }
}
