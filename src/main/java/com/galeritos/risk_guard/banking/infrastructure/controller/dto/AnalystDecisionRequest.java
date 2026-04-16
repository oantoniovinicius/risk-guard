package com.galeritos.risk_guard.banking.infrastructure.controller.dto;

import com.galeritos.risk_guard.banking.domain.model.enums.AnalystDecision;

import jakarta.validation.constraints.NotNull;

public record AnalystDecisionRequest(
        @NotNull AnalystDecision decision) {
}
