package com.galeritos.risk_guard.identity.infrastructure.controller.dto;

import jakarta.validation.constraints.NotBlank;

public record CreatePinRequest(
        @NotBlank String pin) {
}
