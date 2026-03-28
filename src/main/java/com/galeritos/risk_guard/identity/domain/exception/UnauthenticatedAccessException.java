package com.galeritos.risk_guard.identity.domain.exception;

public class UnauthenticatedAccessException extends RuntimeException {
    public UnauthenticatedAccessException() {
        super("Authentication is required.");
    }
}
