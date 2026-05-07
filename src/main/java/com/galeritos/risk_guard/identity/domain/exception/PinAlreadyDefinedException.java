package com.galeritos.risk_guard.identity.domain.exception;

public class PinAlreadyDefinedException extends RuntimeException {
    public PinAlreadyDefinedException() {
        super("PIN already provided.");
    }
}
