package com.galeritos.risk_guard.admin.domain.exception;

import java.util.UUID;

import com.galeritos.risk_guard.identity.domain.model.enums.Role;

public class InvalidRoleChangeException extends RuntimeException {
    public InvalidRoleChangeException(UUID userId, Role currentRole, Role requestedRole) {
        super("Invalid role change for user " + userId + ": " + currentRole + " -> " + requestedRole);
    }
}
