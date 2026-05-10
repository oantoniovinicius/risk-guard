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
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import com.galeritos.risk_guard.banking.application.event.TransactionStatusChangedEvent;
import com.galeritos.risk_guard.banking.application.port.out.BankingEventPublisher;
import com.galeritos.risk_guard.banking.domain.exception.InvalidDisputeStateException;
import com.galeritos.risk_guard.banking.domain.exception.TransactionNotFoundException;
import com.galeritos.risk_guard.banking.domain.model.Transaction;
import com.galeritos.risk_guard.banking.domain.model.enums.FinancialStatus;
import com.galeritos.risk_guard.banking.domain.model.enums.TransactionStatus;
import com.galeritos.risk_guard.banking.infrastructure.persistence.repository.TransactionRepository;
import com.galeritos.risk_guard.shared.events.EventTypes;

@ExtendWith(MockitoExtension.class)
class OpenDisputeUseCaseTest {

    @Mock
    private TransactionRepository transactionRepository;

    @Mock
    private BankingEventPublisher eventPublisher;

    @InjectMocks
    private OpenDisputeUseCase useCase;

    @Test
    void shouldTransitionToDisputedWhenTransactionIsApproved() {
        UUID transactionId = UUID.randomUUID();
        Transaction transaction = new Transaction(
                UUID.randomUUID(),
                UUID.randomUUID(),
                new BigDecimal("200.00"),
                TransactionStatus.APPROVED,
                FinancialStatus.SETTLED,
                null,
                LocalDateTime.now());
        ReflectionTestUtils.setField(transaction, "id", transactionId);

        when(transactionRepository.findByIdForUpdate(transactionId)).thenReturn(Optional.of(transaction));

        useCase.execute(transactionId);

        assertEquals(TransactionStatus.DISPUTED, transaction.getStatus());
        verify(transactionRepository).save(transaction);
        verify(eventPublisher).publishTransactionStatusChanged(any());
    }

    @Test
    void shouldPublishDisputedEvent() {
        UUID transactionId = UUID.randomUUID();
        Transaction transaction = new Transaction(
                UUID.randomUUID(),
                UUID.randomUUID(),
                new BigDecimal("150.00"),
                TransactionStatus.APPROVED,
                FinancialStatus.SETTLED,
                null,
                LocalDateTime.now());
        ReflectionTestUtils.setField(transaction, "id", transactionId);

        when(transactionRepository.findByIdForUpdate(transactionId)).thenReturn(Optional.of(transaction));

        useCase.execute(transactionId);

        ArgumentCaptor<TransactionStatusChangedEvent> captor = ArgumentCaptor.forClass(TransactionStatusChangedEvent.class);
        verify(eventPublisher).publishTransactionStatusChanged(captor.capture());
        assertEquals(EventTypes.TRANSACTION_DISPUTED, captor.getValue().eventType());
        assertEquals(transactionId, captor.getValue().aggregateId());
    }

    @Test
    void shouldThrowWhenTransactionIsNotApproved() {
        UUID transactionId = UUID.randomUUID();
        Transaction transaction = new Transaction(
                UUID.randomUUID(),
                UUID.randomUUID(),
                new BigDecimal("300.00"),
                TransactionStatus.AWAITING_ANALYST,
                FinancialStatus.RESERVED,
                null,
                LocalDateTime.now());
        ReflectionTestUtils.setField(transaction, "id", transactionId);

        when(transactionRepository.findByIdForUpdate(transactionId)).thenReturn(Optional.of(transaction));

        assertThrows(InvalidDisputeStateException.class, () -> useCase.execute(transactionId));

        verify(transactionRepository, never()).save(any());
        verify(eventPublisher, never()).publishTransactionStatusChanged(any());
    }

    @Test
    void shouldThrowWhenTransactionAlreadyDisputed() {
        UUID transactionId = UUID.randomUUID();
        Transaction transaction = new Transaction(
                UUID.randomUUID(),
                UUID.randomUUID(),
                new BigDecimal("100.00"),
                TransactionStatus.DISPUTED,
                FinancialStatus.SETTLED,
                null,
                LocalDateTime.now());
        ReflectionTestUtils.setField(transaction, "id", transactionId);

        when(transactionRepository.findByIdForUpdate(transactionId)).thenReturn(Optional.of(transaction));

        assertThrows(InvalidDisputeStateException.class, () -> useCase.execute(transactionId));

        verify(transactionRepository, never()).save(any());
        verify(eventPublisher, never()).publishTransactionStatusChanged(any());
    }

    @Test
    void shouldThrowWhenTransactionNotFound() {
        UUID transactionId = UUID.randomUUID();
        when(transactionRepository.findByIdForUpdate(transactionId)).thenReturn(Optional.empty());

        assertThrows(TransactionNotFoundException.class, () -> useCase.execute(transactionId));

        verify(transactionRepository, never()).save(any());
        verify(eventPublisher, never()).publishTransactionStatusChanged(any());
    }
}
