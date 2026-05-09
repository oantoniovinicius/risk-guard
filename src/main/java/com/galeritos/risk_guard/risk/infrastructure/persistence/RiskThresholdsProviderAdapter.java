package com.galeritos.risk_guard.risk.infrastructure.persistence;

import java.math.BigDecimal;

import org.springframework.stereotype.Service;

import com.galeritos.risk_guard.admin.infrastructure.persistence.repository.AdminSettingsRepository;
import com.galeritos.risk_guard.config.AdminSettingsDefaultsProperties;
import com.galeritos.risk_guard.risk.application.port.out.RiskThresholdsProvider;

@Service
public class RiskThresholdsProviderAdapter implements RiskThresholdsProvider {

    private final AdminSettingsRepository adminSettingsRepository;
    private final AdminSettingsDefaultsProperties defaults;

    public RiskThresholdsProviderAdapter(
            AdminSettingsRepository adminSettingsRepository,
            AdminSettingsDefaultsProperties defaults) {
        this.adminSettingsRepository = adminSettingsRepository;
        this.defaults = defaults;
    }

    @Override
    public BigDecimal mediumRiskThreshold() {
        return adminSettingsRepository.findAll().stream()
                .findFirst()
                .map(s -> s.getMediumRiskThreshold())
                .orElse(defaults.mediumRiskThreshold());
    }

    @Override
    public BigDecimal highRiskThreshold() {
        return adminSettingsRepository.findAll().stream()
                .findFirst()
                .map(s -> s.getHighRiskThreshold())
                .orElse(defaults.highRiskThreshold());
    }
}
