package com.galeritos.risk_guard.admin.infrastructure.controller.mapper;

import com.galeritos.risk_guard.admin.domain.model.AdminSettings;
import com.galeritos.risk_guard.admin.infrastructure.controller.dto.AdminSettingsResponse;

public final class AdminSettingsMapper {

    private AdminSettingsMapper() {
    }

    public static AdminSettingsResponse toResponse(AdminSettings settings) {
        return new AdminSettingsResponse(
                settings.getMediumRiskThreshold(),
                settings.getHighRiskThreshold(),
                settings.getUpdatedAt());
    }
}
