package com.galeritos.risk_guard.identity.domain.exception;

public class UserCredentialDisabledException extends RuntimeException {
    public UserCredentialDisabledException() {
        super("User credential is disabled.");
    }
}
