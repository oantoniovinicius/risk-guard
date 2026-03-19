package com.galeritos.risk_guard.banking.application.usecase;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Optional;
import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import com.galeritos.risk_guard.banking.domain.model.Transaction;
import com.galeritos.risk_guard.banking.domain.model.enums.FinancialStatus;
import com.galeritos.risk_guard.banking.domain.model.enums.TransactionStatus;
import com.galeritos.risk_guard.banking.infrastructure.persistence.repository.TransactionRepository;
import com.galeritos.risk_guard.risk.application.event.TransactionAnalyzedEvent;
import com.galeritos.risk_guard.shared.enums.RiskLevel;
import com.galeritos.risk_guard.shared.events.EventTypes;

@ExtendWith(MockitoExtension.class)
class ApplyTransactionAnalysisUseCaseTest {

    @Mock
    private TransactionRepository transactionRepository;

    @Mock
    private FinalizeTransactionFinancialUseCase finalizeTransactionFinancialUseCase;

    @InjectMocks
    private ApplyTransactionAnalysisUseCase useCase;

    @Test
    void shouldApproveWhenRiskIsLow() {
        UUID transactionId = UUID.randomUUID();
        Transaction transaction = new Transaction(
                UUID.randomUUID(),
                UUID.randomUUID(),
                new BigDecimal("120.00"),
                TransactionStatus.CREATED,
                FinancialStatus.RESERVED,
                null,
                LocalDateTime.now());
        ReflectionTestUtils.setField(transaction, "id", transactionId);

        TransactionAnalyzedEvent event = new TransactionAnalyzedEvent(
                UUID.randomUUID(),
                EventTypes.TRANSACTION_ANALYZED,
                transactionId,
                RiskLevel.LOW,
                new BigDecimal("0.10"),
                "low");

        when(transactionRepository.findById(transactionId)).thenReturn(Optional.of(transaction));

        useCase.execute(event);

        assertEquals(TransactionStatus.APPROVED, transaction.getStatus());
        assertEquals(RiskLevel.LOW, transaction.getRiskLevel());
        verify(transactionRepository).save(transaction);
        verify(finalizeTransactionFinancialUseCase).execute(transactionId);
    }

    @Test
    void shouldRouteToAnalystWhenRiskIsMedium() {
        UUID transactionId = UUID.randomUUID();
        Transaction transaction = new Transaction(
                UUID.randomUUID(),
                UUID.randomUUID(),
                new BigDecimal("500.00"),
                TransactionStatus.CREATED,
                FinancialStatus.RESERVED,
                null,
                LocalDateTime.now());
        ReflectionTestUtils.setField(transaction, "id", transactionId);

        TransactionAnalyzedEvent event = new TransactionAnalyzedEvent(
                UUID.randomUUID(),
                EventTypes.TRANSACTION_ANALYZED,
                transactionId,
                RiskLevel.MEDIUM,
                new BigDecimal("0.61"),
                "medium");

        when(transactionRepository.findById(transactionId)).thenReturn(Optional.of(transaction));

        useCase.execute(event);

        assertEquals(TransactionStatus.AWAITING_ANALYST, transaction.getStatus());
        assertEquals(RiskLevel.MEDIUM, transaction.getRiskLevel());
        verify(transactionRepository).save(transaction);
        verify(finalizeTransactionFinancialUseCase, never()).execute(any(UUID.class));
    }

    @Test
    void shouldRouteToCustomerWhenRiskIsHigh() {
        UUID transactionId = UUID.randomUUID();
        Transaction transaction = new Transaction(
                UUID.randomUUID(),
                UUID.randomUUID(),
                new BigDecimal("2500.00"),
                TransactionStatus.CREATED,
                FinancialStatus.RESERVED,
                null,
                LocalDateTime.now());
        ReflectionTestUtils.setField(transaction, "id", transactionId);

        TransactionAnalyzedEvent event = new TransactionAnalyzedEvent(
                UUID.randomUUID(),
                EventTypes.TRANSACTION_ANALYZED,
                transactionId,
                RiskLevel.HIGH,
                new BigDecimal("0.92"),
                "high");

        when(transactionRepository.findById(transactionId)).thenReturn(Optional.of(transaction));

        useCase.execute(event);

        assertEquals(TransactionStatus.AWAITING_CUSTOMER, transaction.getStatus());
        assertEquals(RiskLevel.HIGH, transaction.getRiskLevel());
        verify(transactionRepository).save(transaction);
        verify(finalizeTransactionFinancialUseCase, never()).execute(any(UUID.class));
    }

    @Test
    void shouldIgnoreWhenTransactionAlreadyProcessed() {
        UUID transactionId = UUID.randomUUID();
        Transaction transaction = new Transaction(
                UUID.randomUUID(),
                UUID.randomUUID(),
                new BigDecimal("120.00"),
                TransactionStatus.APPROVED,
                FinancialStatus.RESERVED,
                RiskLevel.LOW,
                LocalDateTime.now());
        ReflectionTestUtils.setField(transaction, "id", transactionId);

        TransactionAnalyzedEvent event = new TransactionAnalyzedEvent(
                UUID.randomUUID(),
                EventTypes.TRANSACTION_ANALYZED,
                transactionId,
                RiskLevel.HIGH,
                new BigDecimal("0.92"),
                "duplicate");

        when(transactionRepository.findById(transactionId)).thenReturn(Optional.of(transaction));

        useCase.execute(event);

        assertEquals(TransactionStatus.APPROVED, transaction.getStatus());
        assertEquals(RiskLevel.LOW, transaction.getRiskLevel());
        verify(transactionRepository, never()).save(transaction);
        verify(finalizeTransactionFinancialUseCase, never()).execute(any(UUID.class));
    }

    @Test
    void shouldThrowWhenTransactionDoesNotExist() {
        UUID transactionId = UUID.randomUUID();
        TransactionAnalyzedEvent event = new TransactionAnalyzedEvent(
                UUID.randomUUID(),
                EventTypes.TRANSACTION_ANALYZED,
                transactionId,
                RiskLevel.LOW,
                new BigDecimal("0.10"),
                "missing");

        when(transactionRepository.findById(transactionId)).thenReturn(Optional.empty());

        assertThrows(IllegalArgumentException.class, () -> useCase.execute(event));
        verify(transactionRepository, never()).save(any(Transaction.class));
        verify(finalizeTransactionFinancialUseCase, never()).execute(any(UUID.class));
    }
}
