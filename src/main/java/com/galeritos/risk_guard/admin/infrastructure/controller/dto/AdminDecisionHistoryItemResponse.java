package com.galeritos.risk_guard.admin.infrastructure.controller.dto;

import java.time.LocalDateTime;
import java.util.UUID;

import com.galeritos.risk_guard.admin.domain.model.enums.AdminDecisionAction;
import com.galeritos.risk_guard.identity.domain.model.enums.Role;
import com.galeritos.risk_guard.identity.domain.model.enums.UserStatus;

public record AdminDecisionHistoryItemResponse(
        UUID id,
        UUID actorUserId,
        Role actorRole,
        UUID targetUserId,
        AdminDecisionAction action,
        UserStatus fromStatus,
        UserStatus toStatus,
        Role fromRole,
        Role toRole,
        String reason,
        LocalDateTime createdAt) {
}
