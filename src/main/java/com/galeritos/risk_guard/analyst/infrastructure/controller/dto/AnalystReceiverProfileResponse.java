package com.galeritos.risk_guard.analyst.infrastructure.controller.dto;

import java.time.LocalDateTime;
import java.util.UUID;

public record AnalystReceiverProfileResponse(
        UUID userId,
        String name,
        String email,
        String status,
        boolean suspect,
        LocalDateTime memberSince) {
}
