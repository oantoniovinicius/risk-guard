package com.galeritos.risk_guard.banking.domain.exception;

public class AnalystConflictOfInterestException extends RuntimeException {
    public AnalystConflictOfInterestException() {
        super("Analyst cannot decide on a transaction in which they are the receiver.");
    }
}
