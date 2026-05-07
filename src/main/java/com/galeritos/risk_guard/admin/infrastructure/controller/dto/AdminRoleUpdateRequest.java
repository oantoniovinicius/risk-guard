package com.galeritos.risk_guard.admin.infrastructure.controller.dto;

import com.galeritos.risk_guard.identity.domain.model.enums.Role;

import jakarta.validation.constraints.NotNull;

public record AdminRoleUpdateRequest(
        @NotNull Role role) {
}
