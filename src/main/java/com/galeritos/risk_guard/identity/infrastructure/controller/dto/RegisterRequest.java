package com.galeritos.risk_guard.identity.infrastructure.controller.dto;

import com.galeritos.risk_guard.identity.domain.model.enums.Role;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record RegisterRequest(
        @NotBlank String name,
        @NotBlank @Email String email,
        @NotBlank @Pattern(regexp = "^\\d{11,14}$", message = "document must have 11 to 14 digits") String document,
        @NotBlank @Size(min = 8, max = 72) String password,
        @NotNull Role role) {
}
