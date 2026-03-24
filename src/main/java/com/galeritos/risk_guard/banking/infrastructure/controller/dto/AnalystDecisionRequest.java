package com.galeritos.risk_guard.banking.infrastructure.controller.dto;

import com.galeritos.risk_guard.banking.domain.model.enums.AnalystDecision;

public record AnalystDecisionRequest(
        AnalystDecision decision) {
}
