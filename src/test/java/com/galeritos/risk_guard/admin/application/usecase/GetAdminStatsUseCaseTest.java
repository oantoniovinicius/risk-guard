package com.galeritos.risk_guard.admin.application.usecase;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.galeritos.risk_guard.admin.application.usecase.dto.AdminStatsResult;
import com.galeritos.risk_guard.banking.domain.model.enums.TransactionStatus;
import com.galeritos.risk_guard.banking.infrastructure.persistence.repository.TransactionRepository;
import com.galeritos.risk_guard.identity.domain.model.enums.UserStatus;
import com.galeritos.risk_guard.identity.infrastructure.persistence.repository.UserRepository;

@ExtendWith(MockitoExtension.class)
class GetAdminStatsUseCaseTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private TransactionRepository transactionRepository;

    private GetAdminStatsUseCase useCase;

    @BeforeEach
    void setUp() {
        useCase = new GetAdminStatsUseCase(userRepository, transactionRepository);
    }

    @Test
    void shouldReturnCorrectAggregatedStats() {
        when(userRepository.count()).thenReturn(10L);
        when(userRepository.countByStatus(UserStatus.PENDING)).thenReturn(2L);
        when(userRepository.countByStatus(UserStatus.ACTIVE)).thenReturn(6L);
        when(userRepository.countByStatus(UserStatus.SUSPENDED)).thenReturn(1L);
        when(userRepository.countByStatus(UserStatus.BLOCKED)).thenReturn(0L);
        when(userRepository.countByStatus(UserStatus.REJECTED)).thenReturn(1L);

        when(transactionRepository.count()).thenReturn(100L);
        when(transactionRepository.countByStatus(TransactionStatus.AWAITING_CUSTOMER)).thenReturn(5L);
        when(transactionRepository.countByStatus(TransactionStatus.AWAITING_ANALYST)).thenReturn(8L);
        when(transactionRepository.countByStatus(TransactionStatus.APPROVED)).thenReturn(70L);
        when(transactionRepository.countByStatus(TransactionStatus.DENIED)).thenReturn(10L);
        when(transactionRepository.countByStatus(TransactionStatus.FRAUD_CONFIRMED)).thenReturn(5L);
        when(transactionRepository.countByStatus(TransactionStatus.DISPUTED)).thenReturn(2L);

        AdminStatsResult result = useCase.execute();

        assertEquals(10L, result.totalUsers());
        assertEquals(2L, result.pendingUsers());
        assertEquals(6L, result.activeUsers());
        assertEquals(1L, result.suspendedUsers());
        assertEquals(0L, result.blockedUsers());
        assertEquals(1L, result.rejectedUsers());
        assertEquals(100L, result.totalTransactions());
        assertEquals(5L, result.awaitingCustomerTransactions());
        assertEquals(8L, result.awaitingAnalystTransactions());
        assertEquals(70L, result.approvedTransactions());
        assertEquals(10L, result.deniedTransactions());
        assertEquals(5L, result.fraudConfirmedTransactions());
        assertEquals(2L, result.disputedTransactions());
        assertEquals(new BigDecimal("5.00"), result.fraudRate());
    }

    @Test
    void shouldReturnZeroFraudRateWhenNoTransactions() {
        when(userRepository.count()).thenReturn(0L);
        when(userRepository.countByStatus(UserStatus.PENDING)).thenReturn(0L);
        when(userRepository.countByStatus(UserStatus.ACTIVE)).thenReturn(0L);
        when(userRepository.countByStatus(UserStatus.SUSPENDED)).thenReturn(0L);
        when(userRepository.countByStatus(UserStatus.BLOCKED)).thenReturn(0L);
        when(userRepository.countByStatus(UserStatus.REJECTED)).thenReturn(0L);
        when(transactionRepository.count()).thenReturn(0L);
        when(transactionRepository.countByStatus(TransactionStatus.AWAITING_CUSTOMER)).thenReturn(0L);
        when(transactionRepository.countByStatus(TransactionStatus.AWAITING_ANALYST)).thenReturn(0L);
        when(transactionRepository.countByStatus(TransactionStatus.APPROVED)).thenReturn(0L);
        when(transactionRepository.countByStatus(TransactionStatus.DENIED)).thenReturn(0L);
        when(transactionRepository.countByStatus(TransactionStatus.FRAUD_CONFIRMED)).thenReturn(0L);
        when(transactionRepository.countByStatus(TransactionStatus.DISPUTED)).thenReturn(0L);

        AdminStatsResult result = useCase.execute();

        assertEquals(BigDecimal.ZERO, result.fraudRate());
        assertEquals(0L, result.totalTransactions());
    }
}
