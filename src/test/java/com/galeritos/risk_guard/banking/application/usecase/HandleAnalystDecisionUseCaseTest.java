package com.galeritos.risk_guard.banking.application.usecase;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
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
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import com.galeritos.risk_guard.banking.application.event.TransactionStatusChangedEvent;
import com.galeritos.risk_guard.banking.domain.exception.InvalidAnalystDecisionStateException;
import com.galeritos.risk_guard.banking.domain.exception.TransactionNotFoundException;
import com.galeritos.risk_guard.banking.domain.model.Transaction;
import com.galeritos.risk_guard.banking.domain.model.enums.AnalystDecision;
import com.galeritos.risk_guard.banking.domain.model.enums.FinancialStatus;
import com.galeritos.risk_guard.banking.domain.model.enums.TransactionStatus;
import com.galeritos.risk_guard.banking.application.port.out.BankingEventPublisher;
import com.galeritos.risk_guard.banking.infrastructure.persistence.repository.TransactionRepository;
import com.galeritos.risk_guard.shared.events.EventTypes;

@ExtendWith(MockitoExtension.class)
class HandleAnalystDecisionUseCaseTest {

    @Mock
    private TransactionRepository transactionRepository;

    @Mock
    private FinalizeTransactionFinancialUseCase finalizeTransactionFinancialUseCase;

    @Mock
    private HandleFraudConfirmedUseCase handleFraudConfirmedUseCase;

    @Mock
    private BankingEventPublisher eventPublisher;

    @InjectMocks
    private HandleAnalystDecisionUseCase useCase;

    @Test
    void shouldApproveAndFinalizeWhenAnalystApproves() {
        UUID transactionId = UUID.randomUUID();
        Transaction transaction = new Transaction(
                UUID.randomUUID(),
                UUID.randomUUID(),
                new BigDecimal("400.00"),
                TransactionStatus.AWAITING_ANALYST,
                FinancialStatus.RESERVED,
                null,
                LocalDateTime.now());
        ReflectionTestUtils.setField(transaction, "id", transactionId);

        when(transactionRepository.findByIdForUpdate(transactionId)).thenReturn(Optional.of(transaction));

        useCase.execute(transactionId, AnalystDecision.APPROVE);

        assertEquals(TransactionStatus.APPROVED, transaction.getStatus());
        assertNull(transaction.getCustomerDecisionDeadlineAt());
        verify(transactionRepository).save(transaction);
        verify(finalizeTransactionFinancialUseCase).execute(transactionId);
        verify(handleFraudConfirmedUseCase, never()).execute(transactionId);
        verify(eventPublisher).publishTransactionStatusChanged(any());
    }

    @Test
    void shouldDenyAndFinalizeWhenAnalystDenies() {
        UUID transactionId = UUID.randomUUID();
        Transaction transaction = new Transaction(
                UUID.randomUUID(),
                UUID.randomUUID(),
                new BigDecimal("180.00"),
                TransactionStatus.AWAITING_ANALYST,
                FinancialStatus.RESERVED,
                null,
                LocalDateTime.now());
        ReflectionTestUtils.setField(transaction, "id", transactionId);

        when(transactionRepository.findByIdForUpdate(transactionId)).thenReturn(Optional.of(transaction));

        useCase.execute(transactionId, AnalystDecision.DENY);

        assertEquals(TransactionStatus.DENIED, transaction.getStatus());
        assertNull(transaction.getCustomerDecisionDeadlineAt());
        verify(transactionRepository).save(transaction);
        verify(finalizeTransactionFinancialUseCase).execute(transactionId);
        verify(handleFraudConfirmedUseCase, never()).execute(transactionId);
        verify(eventPublisher).publishTransactionStatusChanged(any());
    }

    @Test
    void shouldConfirmFraudWhenAnalystConfirmsFraud() {
        UUID transactionId = UUID.randomUUID();
        Transaction transaction = new Transaction(
                UUID.randomUUID(),
                UUID.randomUUID(),
                new BigDecimal("180.00"),
                TransactionStatus.AWAITING_ANALYST,
                FinancialStatus.RESERVED,
                null,
                LocalDateTime.now());
        ReflectionTestUtils.setField(transaction, "id", transactionId);

        when(transactionRepository.findByIdForUpdate(transactionId)).thenReturn(Optional.of(transaction));

        useCase.execute(transactionId, AnalystDecision.CONFIRM_FRAUD);

        assertEquals(TransactionStatus.FRAUD_CONFIRMED, transaction.getStatus());
        assertNull(transaction.getCustomerDecisionDeadlineAt());
        verify(transactionRepository).save(transaction);
        verify(handleFraudConfirmedUseCase).execute(transactionId);
        verify(finalizeTransactionFinancialUseCase, never()).execute(transactionId);
        verify(eventPublisher).publishTransactionStatusChanged(any());
    }

    @Test
    void shouldMoveBackToCustomerWithNewDeadlineWhenAnalystRequestsCustomerConfirmation() {
        UUID transactionId = UUID.randomUUID();
        LocalDateTime before = LocalDateTime.now();
        Transaction transaction = new Transaction(
                UUID.randomUUID(),
                UUID.randomUUID(),
                new BigDecimal("510.00"),
                TransactionStatus.AWAITING_ANALYST,
                FinancialStatus.RESERVED,
                null,
                LocalDateTime.now());
        ReflectionTestUtils.setField(transaction, "id", transactionId);

        when(transactionRepository.findByIdForUpdate(transactionId)).thenReturn(Optional.of(transaction));

        useCase.execute(transactionId, AnalystDecision.REQUEST_CUSTOMER_CONFIRMATION);
        LocalDateTime after = LocalDateTime.now();

        assertEquals(TransactionStatus.AWAITING_CUSTOMER, transaction.getStatus());
        assertNotNull(transaction.getCustomerDecisionDeadlineAt());
        assertTrue(!transaction.getCustomerDecisionDeadlineAt().isBefore(before.plusMinutes(10)));
        assertTrue(!transaction.getCustomerDecisionDeadlineAt().isAfter(after.plusMinutes(10)));
        verify(transactionRepository).save(transaction);
        verify(finalizeTransactionFinancialUseCase, never()).execute(transactionId);
        verify(handleFraudConfirmedUseCase, never()).execute(transactionId);
        verify(eventPublisher).publishTransactionStatusChanged(any());
    }

    @Test
    void shouldBeIdempotentWhenAlreadyApprovedAndDecisionIsApprove() {
        UUID transactionId = UUID.randomUUID();
        Transaction transaction = new Transaction(
                UUID.randomUUID(),
                UUID.randomUUID(),
                new BigDecimal("100.00"),
                TransactionStatus.APPROVED,
                FinancialStatus.SETTLED,
                null,
                LocalDateTime.now());
        ReflectionTestUtils.setField(transaction, "id", transactionId);

        when(transactionRepository.findByIdForUpdate(transactionId)).thenReturn(Optional.of(transaction));

        useCase.execute(transactionId, AnalystDecision.APPROVE);

        verify(transactionRepository, never()).save(transaction);
        verify(finalizeTransactionFinancialUseCase).execute(transactionId);
        verify(handleFraudConfirmedUseCase, never()).execute(transactionId);
        verify(eventPublisher, never()).publishTransactionStatusChanged(any());
    }

    @Test
    void shouldBeIdempotentWhenAlreadyDeniedAndDecisionIsDeny() {
        UUID transactionId = UUID.randomUUID();
        Transaction transaction = new Transaction(
                UUID.randomUUID(),
                UUID.randomUUID(),
                new BigDecimal("100.00"),
                TransactionStatus.DENIED,
                FinancialStatus.REVERTED,
                null,
                LocalDateTime.now());
        ReflectionTestUtils.setField(transaction, "id", transactionId);

        when(transactionRepository.findByIdForUpdate(transactionId)).thenReturn(Optional.of(transaction));

        useCase.execute(transactionId, AnalystDecision.DENY);

        verify(transactionRepository, never()).save(transaction);
        verify(finalizeTransactionFinancialUseCase).execute(transactionId);
        verify(handleFraudConfirmedUseCase, never()).execute(transactionId);
        verify(eventPublisher, never()).publishTransactionStatusChanged(any());
    }

    @Test
    void shouldBeIdempotentWhenAlreadyFraudConfirmedAndDecisionIsConfirmFraud() {
        UUID transactionId = UUID.randomUUID();
        Transaction transaction = new Transaction(
                UUID.randomUUID(),
                UUID.randomUUID(),
                new BigDecimal("100.00"),
                TransactionStatus.FRAUD_CONFIRMED,
                FinancialStatus.REVERTED,
                null,
                LocalDateTime.now());
        ReflectionTestUtils.setField(transaction, "id", transactionId);

        when(transactionRepository.findByIdForUpdate(transactionId)).thenReturn(Optional.of(transaction));

        useCase.execute(transactionId, AnalystDecision.CONFIRM_FRAUD);

        verify(transactionRepository, never()).save(transaction);
        verify(handleFraudConfirmedUseCase).execute(transactionId);
        verify(finalizeTransactionFinancialUseCase, never()).execute(transactionId);
        verify(eventPublisher, never()).publishTransactionStatusChanged(any());
    }

    @Test
    void shouldBeIdempotentWhenAlreadyAwaitingCustomerAndDecisionIsRequestCustomerConfirmation() {
        UUID transactionId = UUID.randomUUID();
        Transaction transaction = new Transaction(
                UUID.randomUUID(),
                UUID.randomUUID(),
                new BigDecimal("100.00"),
                TransactionStatus.AWAITING_CUSTOMER,
                FinancialStatus.RESERVED,
                null,
                LocalDateTime.now());
        ReflectionTestUtils.setField(transaction, "id", transactionId);
        ReflectionTestUtils.setField(transaction, "customerDecisionDeadlineAt", LocalDateTime.now().plusMinutes(3));

        when(transactionRepository.findByIdForUpdate(transactionId)).thenReturn(Optional.of(transaction));

        useCase.execute(transactionId, AnalystDecision.REQUEST_CUSTOMER_CONFIRMATION);

        assertEquals(TransactionStatus.AWAITING_CUSTOMER, transaction.getStatus());
        verify(transactionRepository, never()).save(transaction);
        verify(finalizeTransactionFinancialUseCase, never()).execute(transactionId);
        verify(handleFraudConfirmedUseCase, never()).execute(transactionId);
        verify(eventPublisher, never()).publishTransactionStatusChanged(any());
    }

    @Test
    void shouldThrowWhenTransactionNotFound() {
        UUID transactionId = UUID.randomUUID();
        when(transactionRepository.findByIdForUpdate(transactionId)).thenReturn(Optional.empty());

        assertThrows(TransactionNotFoundException.class, () -> useCase.execute(transactionId, AnalystDecision.APPROVE));

        verify(transactionRepository, never()).save(org.mockito.ArgumentMatchers.any(Transaction.class));
        verify(finalizeTransactionFinancialUseCase, never()).execute(transactionId);
        verify(handleFraudConfirmedUseCase, never()).execute(transactionId);
        verify(eventPublisher, never()).publishTransactionStatusChanged(any());
    }

    @Test
    void shouldThrowWhenDecisionIsInvalidForCurrentState() {
        UUID transactionId = UUID.randomUUID();
        Transaction transaction = new Transaction(
                UUID.randomUUID(),
                UUID.randomUUID(),
                new BigDecimal("100.00"),
                TransactionStatus.AWAITING_CUSTOMER,
                FinancialStatus.RESERVED,
                null,
                LocalDateTime.now());
        ReflectionTestUtils.setField(transaction, "id", transactionId);

        when(transactionRepository.findByIdForUpdate(transactionId)).thenReturn(Optional.of(transaction));

        assertThrows(InvalidAnalystDecisionStateException.class,
                () -> useCase.execute(transactionId, AnalystDecision.DENY));

        verify(transactionRepository, never()).save(transaction);
        verify(finalizeTransactionFinancialUseCase, never()).execute(transactionId);
        verify(handleFraudConfirmedUseCase, never()).execute(transactionId);
        verify(eventPublisher, never()).publishTransactionStatusChanged(any());
    }

    @Test
    void shouldPublishTransactionApprovedEvent() {
        UUID transactionId = UUID.randomUUID();
        Transaction transaction = new Transaction(
                UUID.randomUUID(),
                UUID.randomUUID(),
                new BigDecimal("80.00"),
                TransactionStatus.AWAITING_ANALYST,
                FinancialStatus.RESERVED,
                null,
                LocalDateTime.now());
        ReflectionTestUtils.setField(transaction, "id", transactionId);

        when(transactionRepository.findByIdForUpdate(transactionId)).thenReturn(Optional.of(transaction));

        useCase.execute(transactionId, AnalystDecision.APPROVE);

        ArgumentCaptor<TransactionStatusChangedEvent> captor = ArgumentCaptor.forClass(TransactionStatusChangedEvent.class);
        verify(eventPublisher).publishTransactionStatusChanged(captor.capture());
        assertEquals(EventTypes.TRANSACTION_APPROVED, captor.getValue().eventType());
    }

    @Test
    void shouldPublishTransactionDeniedEvent() {
        UUID transactionId = UUID.randomUUID();
        Transaction transaction = new Transaction(
                UUID.randomUUID(),
                UUID.randomUUID(),
                new BigDecimal("80.00"),
                TransactionStatus.AWAITING_ANALYST,
                FinancialStatus.RESERVED,
                null,
                LocalDateTime.now());
        ReflectionTestUtils.setField(transaction, "id", transactionId);

        when(transactionRepository.findByIdForUpdate(transactionId)).thenReturn(Optional.of(transaction));

        useCase.execute(transactionId, AnalystDecision.DENY);

        ArgumentCaptor<TransactionStatusChangedEvent> captor = ArgumentCaptor.forClass(TransactionStatusChangedEvent.class);
        verify(eventPublisher).publishTransactionStatusChanged(captor.capture());
        assertEquals(EventTypes.TRANSACTION_DENIED, captor.getValue().eventType());
    }

    @Test
    void shouldPublishTransactionFraudConfirmedEvent() {
        UUID transactionId = UUID.randomUUID();
        Transaction transaction = new Transaction(
                UUID.randomUUID(),
                UUID.randomUUID(),
                new BigDecimal("80.00"),
                TransactionStatus.AWAITING_ANALYST,
                FinancialStatus.RESERVED,
                null,
                LocalDateTime.now());
        ReflectionTestUtils.setField(transaction, "id", transactionId);

        when(transactionRepository.findByIdForUpdate(transactionId)).thenReturn(Optional.of(transaction));

        useCase.execute(transactionId, AnalystDecision.CONFIRM_FRAUD);

        ArgumentCaptor<TransactionStatusChangedEvent> captor = ArgumentCaptor.forClass(TransactionStatusChangedEvent.class);
        verify(eventPublisher).publishTransactionStatusChanged(captor.capture());
        assertEquals(EventTypes.TRANSACTION_FRAUD_CONFIRMED, captor.getValue().eventType());
    }
}
