package com.galeritos.risk_guard.admin.infrastructure.controller.dto;

import java.time.LocalDateTime;
import java.util.UUID;

import com.galeritos.risk_guard.identity.domain.model.enums.UserStatus;

public record AdminUserSummaryResponse(
        UUID userId,
        String name,
        String email,
        String document,
        UserStatus status,
        LocalDateTime createdAt) {
}
