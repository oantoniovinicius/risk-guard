package com.galeritos.risk_guard.admin.application.usecase;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.time.LocalTime;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.galeritos.risk_guard.admin.domain.exception.InvalidAdminSettingsException;
import com.galeritos.risk_guard.admin.domain.model.AdminSettings;

@ExtendWith(MockitoExtension.class)
class UpdateAdminSettingsUseCaseTest {
    @Mock
    private GetAdminSettingsUseCase getAdminSettingsUseCase;

    private UpdateAdminSettingsUseCase useCase;

    @BeforeEach
    void setUp() {
        useCase = new UpdateAdminSettingsUseCase(getAdminSettingsUseCase);
    }

    @Test
    void shouldPartiallyUpdateSettings() {
        AdminSettings existing = new AdminSettings(
                UUID.randomUUID(),
                new BigDecimal("0.60"),
                new BigDecimal("0.90"),
                LocalTime.of(8, 0),
                LocalTime.of(18, 0));

        when(getAdminSettingsUseCase.execute()).thenReturn(existing);

        AdminSettings updated = useCase.execute(
                new BigDecimal("0.55"),
                null,
                null,
                LocalTime.of(19, 0));

        assertEquals(0, new BigDecimal("0.55").compareTo(updated.getMediumRiskThreshold()));
        assertEquals(0, new BigDecimal("0.90").compareTo(updated.getHighRiskThreshold()));
        assertEquals(LocalTime.of(8, 0), updated.getBusinessStartTime());
        assertEquals(LocalTime.of(19, 0), updated.getBusinessEndTime());
    }

    @Test
    void shouldFailWhenHighThresholdIsNotGreaterThanMedium() {
        AdminSettings existing = new AdminSettings(
                UUID.randomUUID(),
                new BigDecimal("0.60"),
                new BigDecimal("0.90"),
                LocalTime.of(8, 0),
                LocalTime.of(18, 0));

        when(getAdminSettingsUseCase.execute()).thenReturn(existing);

        assertThrows(InvalidAdminSettingsException.class,
                () -> useCase.execute(new BigDecimal("0.70"), new BigDecimal("0.65"), null, null));
    }
}
