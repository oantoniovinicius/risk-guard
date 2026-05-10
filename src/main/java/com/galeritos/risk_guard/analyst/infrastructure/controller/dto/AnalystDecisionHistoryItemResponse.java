package com.galeritos.risk_guard.analyst.infrastructure.controller.dto;

import java.time.LocalDateTime;
import java.util.UUID;

public record AnalystDecisionHistoryItemResponse(
        UUID analystId,
        String decision,
        String fromStatus,
        String toStatus,
        String reason,
        LocalDateTime decidedAt) {
}
