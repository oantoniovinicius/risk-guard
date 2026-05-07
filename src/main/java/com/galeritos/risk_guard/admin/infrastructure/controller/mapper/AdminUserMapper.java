package com.galeritos.risk_guard.admin.infrastructure.controller.mapper;

import com.galeritos.risk_guard.admin.infrastructure.controller.dto.AdminUserDetailResponse;
import com.galeritos.risk_guard.admin.infrastructure.controller.dto.AdminUserRoleResponse;
import com.galeritos.risk_guard.admin.infrastructure.controller.dto.AdminUserStatusResponse;
import com.galeritos.risk_guard.admin.infrastructure.controller.dto.AdminUserSummaryResponse;
import com.galeritos.risk_guard.identity.domain.model.User;

public final class AdminUserMapper {

    private AdminUserMapper() {
    }

    public static AdminUserSummaryResponse toSummary(User user) {
        return new AdminUserSummaryResponse(
                user.getId(),
                user.getName(),
                user.getEmail(),
                user.getDocument(),
                user.getRole(),
                user.getStatus(),
                user.getCreatedAt());
    }

    public static AdminUserDetailResponse toDetail(User user) {
        return new AdminUserDetailResponse(
                user.getId(),
                user.getName(),
                user.getEmail(),
                user.getDocument(),
                user.getRole(),
                user.getStatus(),
                user.isSuspect(),
                user.getCreatedAt());
    }

    public static AdminUserStatusResponse toStatus(User user) {
        return new AdminUserStatusResponse(user.getId(), user.getStatus());
    }

    public static AdminUserRoleResponse toRole(User user) {
        return new AdminUserRoleResponse(user.getId(), user.getRole());
    }
}
