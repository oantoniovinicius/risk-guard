package com.galeritos.risk_guard.banking.infrastructure.controller.dto;

import com.galeritos.risk_guard.banking.domain.model.enums.CustomerConfirmationDecision;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record CustomerConfirmationRequest(
                @NotNull CustomerConfirmationDecision decision, @NotBlank String pin) {
}
