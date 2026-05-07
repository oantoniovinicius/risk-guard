package com.galeritos.risk_guard.admin.infrastructure.controller.dto;

import java.math.BigDecimal;

public record AdminStatsResponse(
        long totalUsers,
        long pendingUsers,
        long activeUsers,
        long suspendedUsers,
        long blockedUsers,
        long rejectedUsers,
        long totalTransactions,
        long awaitingCustomerTransactions,
        long awaitingAnalystTransactions,
        long approvedTransactions,
        long deniedTransactions,
        long fraudConfirmedTransactions,
        long disputedTransactions,
        BigDecimal fraudRate) {
}
