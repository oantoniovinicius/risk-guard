package com.galeritos.risk_guard.identity.infrastructure.controller.dto;

import java.util.UUID;

import com.galeritos.risk_guard.identity.domain.model.enums.Role;
import com.galeritos.risk_guard.identity.domain.model.enums.UserStatus;

public record RegisterResponse(
        UUID userId,
        String email,
        Role role,
        UserStatus status) {
}
