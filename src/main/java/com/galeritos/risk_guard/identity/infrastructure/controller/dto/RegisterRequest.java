package com.galeritos.risk_guard.identity.infrastructure.controller.dto;

import com.galeritos.risk_guard.shared.infrastructure.validation.ValidBrazilianDocument;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record RegisterRequest(
                @NotBlank String name,
                @NotBlank @Email String email,
                @NotBlank @ValidBrazilianDocument String document,
                @NotBlank @Size(min = 6, max = 72) String password) {
}
