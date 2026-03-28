package com.galeritos.risk_guard.identity.domain.exception;

import java.util.UUID;

import com.galeritos.risk_guard.identity.domain.model.enums.UserStatus;

public class UserNotActiveException extends RuntimeException {
    public UserNotActiveException(UUID userId, UserStatus status) {
        super("User " + userId + " is not ACTIVE. Current status: " + status);
    }
}
