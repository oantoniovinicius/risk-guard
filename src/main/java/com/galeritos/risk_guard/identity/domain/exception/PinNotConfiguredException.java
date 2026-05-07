package com.galeritos.risk_guard.identity.domain.exception;

public class PinNotConfiguredException extends RuntimeException {
    public PinNotConfiguredException() {
        super("User must define PIN before performing this action.");
    }
}
