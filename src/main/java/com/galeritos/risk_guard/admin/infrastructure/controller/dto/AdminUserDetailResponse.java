package com.galeritos.risk_guard.admin.infrastructure.controller.dto;

import java.time.LocalDateTime;
import java.util.UUID;

import com.galeritos.risk_guard.identity.domain.model.enums.Role;
import com.galeritos.risk_guard.identity.domain.model.enums.UserStatus;

public record AdminUserDetailResponse(
        UUID userId,
        String name,
        String email,
        String document,
        Role role,
        UserStatus status,
        boolean suspect,
        LocalDateTime createdAt) {
}
