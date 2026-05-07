package com.galeritos.risk_guard.admin.infrastructure.controller.mapper;

import org.springframework.data.domain.Page;

import com.galeritos.risk_guard.admin.domain.model.AdminDecisionHistory;
import com.galeritos.risk_guard.admin.infrastructure.controller.dto.AdminDecisionHistoryItemResponse;
import com.galeritos.risk_guard.admin.infrastructure.controller.dto.AdminDecisionHistoryResponse;

public final class AdminDecisionHistoryMapper {

    private AdminDecisionHistoryMapper() {
    }

    public static AdminDecisionHistoryItemResponse toItemResponse(AdminDecisionHistory item) {
        return new AdminDecisionHistoryItemResponse(
                item.getId(),
                item.getActorUserId(),
                item.getActorRole(),
                item.getTargetUserId(),
                item.getAction(),
                item.getFromStatus(),
                item.getToStatus(),
                item.getFromRole(),
                item.getToRole(),
                item.getReason(),
                item.getCreatedAt());
    }

    public static AdminDecisionHistoryResponse toPageResponse(Page<AdminDecisionHistory> page) {
        return new AdminDecisionHistoryResponse(
                page.getNumber(),
                page.getSize(),
                page.getTotalElements(),
                page.getTotalPages(),
                page.getContent().stream().map(AdminDecisionHistoryMapper::toItemResponse).toList());
    }
}
