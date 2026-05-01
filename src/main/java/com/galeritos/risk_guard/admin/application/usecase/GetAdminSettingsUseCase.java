package com.galeritos.risk_guard.admin.application.usecase;

import java.util.UUID;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.galeritos.risk_guard.admin.domain.model.AdminSettings;
import com.galeritos.risk_guard.admin.infrastructure.persistence.repository.AdminSettingsRepository;
import com.galeritos.risk_guard.config.AdminSettingsDefaultsProperties;

@Service
public class GetAdminSettingsUseCase {
    private static final UUID SETTINGS_ID = UUID.fromString("00000000-0000-0000-0000-000000000001");

    private final AdminSettingsRepository repository;
    private final AdminSettingsDefaultsProperties defaults;

    public GetAdminSettingsUseCase(AdminSettingsRepository repository, AdminSettingsDefaultsProperties defaults) {
        this.repository = repository;
        this.defaults = defaults;
    }

    @Transactional
    public AdminSettings execute() {
        return repository.findById(SETTINGS_ID)
                .orElseGet(() -> repository.save(new AdminSettings(
                        SETTINGS_ID,
                        defaults.mediumRiskThreshold(),
                        defaults.highRiskThreshold(),
                        defaults.businessStartTime(),
                        defaults.businessEndTime())));
    }
}
