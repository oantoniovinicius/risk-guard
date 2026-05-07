package com.galeritos.risk_guard.admin.infrastructure.controller.mapper;

import com.galeritos.risk_guard.admin.application.usecase.dto.AdminStatsResult;
import com.galeritos.risk_guard.admin.infrastructure.controller.dto.AdminStatsResponse;

public final class AdminStatsMapper {

    private AdminStatsMapper() {
    }

    public static AdminStatsResponse toResponse(AdminStatsResult result) {
        return new AdminStatsResponse(
                result.totalUsers(),
                result.pendingUsers(),
                result.activeUsers(),
                result.suspendedUsers(),
                result.blockedUsers(),
                result.rejectedUsers(),
                result.totalTransactions(),
                result.awaitingCustomerTransactions(),
                result.awaitingAnalystTransactions(),
                result.approvedTransactions(),
                result.deniedTransactions(),
                result.fraudConfirmedTransactions(),
                result.disputedTransactions(),
                result.fraudRate());
    }
}
