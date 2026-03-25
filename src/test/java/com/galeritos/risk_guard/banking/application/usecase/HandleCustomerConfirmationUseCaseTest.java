package com.galeritos.risk_guard.banking.application.usecase;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
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

import com.galeritos.risk_guard.banking.domain.exception.InvalidCustomerConfirmationStateException;
import com.galeritos.risk_guard.banking.domain.exception.TransactionNotFoundException;
import com.galeritos.risk_guard.banking.domain.model.Transaction;
import com.galeritos.risk_guard.banking.domain.model.enums.CustomerConfirmationDecision;
import com.galeritos.risk_guard.banking.domain.model.enums.FinancialStatus;
import com.galeritos.risk_guard.banking.domain.model.enums.TransactionStatus;
import com.galeritos.risk_guard.banking.application.port.out.BankingEventPublisher;
import com.galeritos.risk_guard.banking.infrastructure.persistence.repository.TransactionRepository;

@ExtendWith(MockitoExtension.class)
class HandleCustomerConfirmationUseCaseTest {

    @Mock
    private TransactionRepository transactionRepository;

    @Mock
    private FinalizeTransactionFinancialUseCase finalizeTransactionFinancialUseCase;

    @Mock
    private HandleFraudConfirmedUseCase handleFraudConfirmedUseCase;

    @Mock
    private BankingEventPublisher eventPublisher;

    @InjectMocks
    private HandleCustomerConfirmationUseCase useCase;

    @Test
    void shouldApproveAndFinalizeWhenCustomerConfirms() {
        UUID transactionId = UUID.randomUUID();

        Transaction transaction = new Transaction(
                UUID.randomUUID(),
                UUID.randomUUID(),
                new BigDecimal("300.00"),
                TransactionStatus.AWAITING_CUSTOMER,
                FinancialStatus.RESERVED,
                null,
                LocalDateTime.now());
        ReflectionTestUtils.setField(transaction, "id", transactionId);
        ReflectionTestUtils.setField(transaction, "customerDecisionDeadlineAt", LocalDateTime.now().plusMinutes(2));

        when(transactionRepository.findByIdForUpdate(transactionId)).thenReturn(Optional.of(transaction));

        useCase.execute(transactionId, CustomerConfirmationDecision.APPROVE);

        assertEquals(TransactionStatus.APPROVED, transaction.getStatus());
        assertNull(transaction.getCustomerDecisionDeadlineAt());
        verify(transactionRepository).save(transaction);
        verify(finalizeTransactionFinancialUseCase).execute(transactionId);
        verify(handleFraudConfirmedUseCase, never()).execute(transactionId);
        verify(eventPublisher).publishTransactionStatusChanged(any());
    }

    @Test
    void shouldConfirmFraudWhenCustomerReportsFraud() {
        UUID transactionId = UUID.randomUUID();

        Transaction transaction = new Transaction(
                UUID.randomUUID(),
                UUID.randomUUID(),
                new BigDecimal("145.00"),
                TransactionStatus.AWAITING_CUSTOMER,
                FinancialStatus.RESERVED,
                null,
                LocalDateTime.now());
        ReflectionTestUtils.setField(transaction, "id", transactionId);
        ReflectionTestUtils.setField(transaction, "customerDecisionDeadlineAt", LocalDateTime.now().plusMinutes(4));

        when(transactionRepository.findByIdForUpdate(transactionId)).thenReturn(Optional.of(transaction));

        useCase.execute(transactionId, CustomerConfirmationDecision.REPORT_FRAUD);

        assertEquals(TransactionStatus.FRAUD_CONFIRMED, transaction.getStatus());
        assertNull(transaction.getCustomerDecisionDeadlineAt());
        verify(transactionRepository).save(transaction);
        verify(handleFraudConfirmedUseCase).execute(transactionId);
        verify(finalizeTransactionFinancialUseCase, never()).execute(transactionId);
        verify(eventPublisher).publishTransactionStatusChanged(any());
    }

    @Test
    void shouldBeIdempotentWhenAlreadyApprovedAndDecisionIsApprove() {
        UUID transactionId = UUID.randomUUID();
        Transaction transaction = new Transaction(
                UUID.randomUUID(),
                UUID.randomUUID(),
                new BigDecimal("90.00"),
                TransactionStatus.APPROVED,
                FinancialStatus.SETTLED,
                null,
                LocalDateTime.now());
        ReflectionTestUtils.setField(transaction, "id", transactionId);

        when(transactionRepository.findByIdForUpdate(transactionId)).thenReturn(Optional.of(transaction));

        useCase.execute(transactionId, CustomerConfirmationDecision.APPROVE);

        assertEquals(TransactionStatus.APPROVED, transaction.getStatus());
        verify(transactionRepository, never()).save(transaction);
        verify(finalizeTransactionFinancialUseCase).execute(transactionId);
        verify(handleFraudConfirmedUseCase, never()).execute(transactionId);
        verify(eventPublisher, never()).publishTransactionStatusChanged(any());
    }

    @Test
    void shouldBeIdempotentWhenAlreadyFraudConfirmedAndDecisionIsReportFraud() {
        UUID transactionId = UUID.randomUUID();
        Transaction transaction = new Transaction(
                UUID.randomUUID(),
                UUID.randomUUID(),
                new BigDecimal("90.00"),
                TransactionStatus.FRAUD_CONFIRMED,
                FinancialStatus.REVERTED,
                null,
                LocalDateTime.now());
        ReflectionTestUtils.setField(transaction, "id", transactionId);

        when(transactionRepository.findByIdForUpdate(transactionId)).thenReturn(Optional.of(transaction));

        useCase.execute(transactionId, CustomerConfirmationDecision.REPORT_FRAUD);

        assertEquals(TransactionStatus.FRAUD_CONFIRMED, transaction.getStatus());
        verify(transactionRepository, never()).save(transaction);
        verify(handleFraudConfirmedUseCase).execute(transactionId);
        verify(finalizeTransactionFinancialUseCase, never()).execute(transactionId);
        verify(eventPublisher, never()).publishTransactionStatusChanged(any());
    }

    @Test
    void shouldThrowWhenTransactionNotFound() {
        UUID transactionId = UUID.randomUUID();

        when(transactionRepository.findByIdForUpdate(transactionId)).thenReturn(Optional.empty());

        assertThrows(TransactionNotFoundException.class,
                () -> useCase.execute(transactionId, CustomerConfirmationDecision.APPROVE));

        verify(transactionRepository, never()).save(org.mockito.ArgumentMatchers.any(Transaction.class));
        verify(finalizeTransactionFinancialUseCase, never()).execute(transactionId);
        verify(handleFraudConfirmedUseCase, never()).execute(transactionId);
        verify(eventPublisher, never()).publishTransactionStatusChanged(any());
    }

    @Test
    void shouldThrowWhenTransactionIsNotAwaitingCustomerAndDecisionIsNotIdempotent() {
        UUID transactionId = UUID.randomUUID();

        Transaction transaction = new Transaction(
                UUID.randomUUID(),
                UUID.randomUUID(),
                new BigDecimal("77.00"),
                TransactionStatus.AWAITING_ANALYST,
                FinancialStatus.RESERVED,
                null,
                LocalDateTime.now());
        ReflectionTestUtils.setField(transaction, "id", transactionId);

        when(transactionRepository.findByIdForUpdate(transactionId)).thenReturn(Optional.of(transaction));

        assertThrows(InvalidCustomerConfirmationStateException.class,
                () -> useCase.execute(transactionId, CustomerConfirmationDecision.APPROVE));

        verify(transactionRepository, never()).save(transaction);
        verify(finalizeTransactionFinancialUseCase, never()).execute(transactionId);
        verify(handleFraudConfirmedUseCase, never()).execute(transactionId);
        verify(eventPublisher, never()).publishTransactionStatusChanged(any());
    }
}
