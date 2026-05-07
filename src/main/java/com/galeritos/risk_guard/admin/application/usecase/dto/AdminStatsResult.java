package com.galeritos.risk_guard.admin.application.usecase.dto;

import java.math.BigDecimal;

public record AdminStatsResult(
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
