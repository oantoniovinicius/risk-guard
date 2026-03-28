package com.galeritos.risk_guard.identity.application.usecase.dto;

import com.galeritos.risk_guard.identity.domain.model.enums.Role;

public record RegisterUserCommand(
        String name,
        String email,
        String document,
        String password,
        Role role) {
}
