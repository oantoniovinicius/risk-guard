package com.galeritos.risk_guard.identity.domain.exception;

public class ForbiddenTransferOperationException extends RuntimeException {
    public ForbiddenTransferOperationException() {
        super("You are not allowed to perform this transfer operation.");
    }
}
