package com.galeritos.risk_guard.banking.infrastructure.controller.dto;

import java.util.List;

public record UserTransferPageResponse(
        int page,
        int size,
        long totalElements,
        int totalPages,
        List<UserTransferResponse> content) {
}
