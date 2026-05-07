package com.galeritos.risk_guard.admin.application.usecase;

import java.math.BigDecimal;
import java.time.LocalTime;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.galeritos.risk_guard.admin.domain.exception.InvalidAdminSettingsException;
import com.galeritos.risk_guard.admin.domain.model.AdminSettings;

@Service
public class UpdateAdminSettingsUseCase {
    private final GetAdminSettingsUseCase getAdminSettingsUseCase;

    public UpdateAdminSettingsUseCase(GetAdminSettingsUseCase getAdminSettingsUseCase) {
        this.getAdminSettingsUseCase = getAdminSettingsUseCase;
    }

    @Transactional
    public AdminSettings execute(
            BigDecimal mediumRiskThreshold,
            BigDecimal highRiskThreshold,
            LocalTime businessStartTime,
            LocalTime businessEndTime) {
        AdminSettings current = getAdminSettingsUseCase.execute();

        BigDecimal newMedium = mediumRiskThreshold != null ? mediumRiskThreshold : current.getMediumRiskThreshold();
        BigDecimal newHigh = highRiskThreshold != null ? highRiskThreshold : current.getHighRiskThreshold();
        LocalTime newStart = businessStartTime != null ? businessStartTime : current.getBusinessStartTime();
        LocalTime newEnd = businessEndTime != null ? businessEndTime : current.getBusinessEndTime();

        validate(newMedium, newHigh, newStart, newEnd);
        current.update(newMedium, newHigh, newStart, newEnd);
        return current;
    }

    private void validate(BigDecimal medium, BigDecimal high, LocalTime start, LocalTime end) {
        if (medium.compareTo(BigDecimal.ZERO) < 0 || medium.compareTo(BigDecimal.ONE) > 0) {
            throw new InvalidAdminSettingsException("mediumRiskThreshold must be between 0 and 1");
        }

        if (high.compareTo(BigDecimal.ZERO) < 0 || high.compareTo(BigDecimal.ONE) > 0) {
            throw new InvalidAdminSettingsException("highRiskThreshold must be between 0 and 1");
        }

        if (high.compareTo(medium) <= 0) {
            throw new InvalidAdminSettingsException("highRiskThreshold must be greater than mediumRiskThreshold");
        }

        if (!start.isBefore(end)) {
            throw new InvalidAdminSettingsException("businessStartTime must be before businessEndTime");
        }
    }
}
