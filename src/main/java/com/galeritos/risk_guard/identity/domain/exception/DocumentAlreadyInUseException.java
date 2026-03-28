package com.galeritos.risk_guard.identity.domain.exception;

public class DocumentAlreadyInUseException extends RuntimeException {
    public DocumentAlreadyInUseException(String document) {
        super("Document already in use: " + document);
    }
}
