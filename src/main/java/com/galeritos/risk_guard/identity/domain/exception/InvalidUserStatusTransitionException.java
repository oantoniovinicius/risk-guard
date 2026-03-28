package com.galeritos.risk_guard.identity.domain.exception;

import com.galeritos.risk_guard.identity.domain.model.enums.UserStatus;

public class InvalidUserStatusTransitionException extends RuntimeException {
    public InvalidUserStatusTransitionException(UserStatus from, UserStatus to) {
        super("Invalid user status transition from " + from + " to " + to);
    }
}
