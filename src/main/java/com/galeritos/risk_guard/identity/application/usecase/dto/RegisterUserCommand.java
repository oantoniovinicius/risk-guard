package com.galeritos.risk_guard.identity.application.usecase.dto;

public record RegisterUserCommand(
                String name,
                String email,
                String document,
                String password) {
}
