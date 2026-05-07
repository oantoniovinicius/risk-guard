package com.galeritos.risk_guard.admin.infrastructure.controller.dto;

import java.util.List;

public record AdminUserPageResponse(
        int page,
        int size,
        long totalElements,
        int totalPages,
        List<AdminUserSummaryResponse> items) {
}
