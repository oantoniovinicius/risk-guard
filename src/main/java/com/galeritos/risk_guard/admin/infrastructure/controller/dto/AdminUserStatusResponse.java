package com.galeritos.risk_guard.admin.infrastructure.controller.dto;

import java.util.UUID;

import com.galeritos.risk_guard.identity.domain.model.enums.UserStatus;

public record AdminUserStatusResponse(UUID userId, UserStatus status) {
}
