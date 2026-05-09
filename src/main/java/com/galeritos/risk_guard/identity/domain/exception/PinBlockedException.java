package com.galeritos.risk_guard.identity.domain.exception;

public class PinBlockedException extends RuntimeException {
    public PinBlockedException() {
        super("PIN blocked due to too many failed attempts.");
    }
}
