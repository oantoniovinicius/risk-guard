package com.galeritos.risk_guard.identity.application.usecase.dto;

import java.util.UUID;

import com.galeritos.risk_guard.identity.domain.model.enums.Role;
import com.galeritos.risk_guard.identity.domain.model.enums.UserStatus;

public record LoginResult(
        String accessToken,
        UUID userId,
        String email,
        Role role,
        UserStatus status) {
}
