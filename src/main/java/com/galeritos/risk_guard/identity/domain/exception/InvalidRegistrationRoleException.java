package com.galeritos.risk_guard.identity.domain.exception;

import com.galeritos.risk_guard.identity.domain.model.enums.Role;

public class InvalidRegistrationRoleException extends RuntimeException {
    public InvalidRegistrationRoleException(Role role) {
        super("Self registration is only allowed for role USER. Requested role: " + role);
    }
}
