package com.galeritos.risk_guard.admin.application.usecase;

import java.math.BigDecimal;
import java.math.RoundingMode;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.galeritos.risk_guard.admin.application.usecase.dto.AdminStatsResult;
import com.galeritos.risk_guard.banking.domain.model.enums.TransactionStatus;
import com.galeritos.risk_guard.banking.infrastructure.persistence.repository.TransactionRepository;
import com.galeritos.risk_guard.identity.domain.model.enums.UserStatus;
import com.galeritos.risk_guard.identity.infrastructure.persistence.repository.UserRepository;

@Service
public class GetAdminStatsUseCase {

    private final UserRepository userRepository;
    private final TransactionRepository transactionRepository;

    public GetAdminStatsUseCase(UserRepository userRepository, TransactionRepository transactionRepository) {
        this.userRepository = userRepository;
        this.transactionRepository = transactionRepository;
    }

    @Transactional(readOnly = true)
    public AdminStatsResult execute() {
        long totalUsers = userRepository.count();
        long pendingUsers = userRepository.countByStatus(UserStatus.PENDING);
        long activeUsers = userRepository.countByStatus(UserStatus.ACTIVE);
        long suspendedUsers = userRepository.countByStatus(UserStatus.SUSPENDED);
        long blockedUsers = userRepository.countByStatus(UserStatus.BLOCKED);
        long rejectedUsers = userRepository.countByStatus(UserStatus.REJECTED);

        long totalTransactions = transactionRepository.count();
        long awaitingCustomer = transactionRepository.countByStatus(TransactionStatus.AWAITING_CUSTOMER);
        long awaitingAnalyst = transactionRepository.countByStatus(TransactionStatus.AWAITING_ANALYST);
        long approved = transactionRepository.countByStatus(TransactionStatus.APPROVED);
        long denied = transactionRepository.countByStatus(TransactionStatus.DENIED);
        long fraudConfirmed = transactionRepository.countByStatus(TransactionStatus.FRAUD_CONFIRMED);
        long disputed = transactionRepository.countByStatus(TransactionStatus.DISPUTED);

        BigDecimal fraudRate = totalTransactions == 0 ? BigDecimal.ZERO
                : BigDecimal.valueOf(fraudConfirmed)
                        .divide(BigDecimal.valueOf(totalTransactions), 4, RoundingMode.HALF_UP)
                        .multiply(BigDecimal.valueOf(100))
                        .setScale(2, RoundingMode.HALF_UP);

        return new AdminStatsResult(
                totalUsers, pendingUsers, activeUsers, suspendedUsers, blockedUsers, rejectedUsers,
                totalTransactions, awaitingCustomer, awaitingAnalyst, approved, denied, fraudConfirmed,
                disputed, fraudRate);
    }
}
