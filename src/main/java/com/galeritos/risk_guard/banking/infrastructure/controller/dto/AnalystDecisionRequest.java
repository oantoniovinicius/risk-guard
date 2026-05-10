package com.galeritos.risk_guard.banking.infrastructure.controller.dto;

import com.galeritos.risk_guard.banking.domain.model.enums.AnalystDecision;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record AnalystDecisionRequest(
        @NotNull AnalystDecision decision,
        @Size(max = 500) String reason) {
}
