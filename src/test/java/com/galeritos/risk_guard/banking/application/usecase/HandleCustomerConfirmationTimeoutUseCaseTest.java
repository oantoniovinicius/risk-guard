package com.galeritos.risk_guard.banking.application.usecase;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
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

@ExtendWith(MockitoExtension.class)
class HandleCustomerConfirmationTimeoutUseCaseTest {

    @Mock
    private TransactionRepository transactionRepository;

    @InjectMocks
    private HandleCustomerConfirmationTimeoutUseCase useCase;

    @Test
    void shouldMoveTransactionToAwaitingAnalystWhenCustomerConfirmationDeadlineExpires() {
        UUID transactionId = UUID.randomUUID();
        LocalDateTime now = LocalDateTime.now();

        Transaction transaction = new Transaction(
                UUID.randomUUID(),
                UUID.randomUUID(),
                new BigDecimal("320.00"),
                TransactionStatus.AWAITING_CUSTOMER,
                FinancialStatus.RESERVED,
                null,
                LocalDateTime.now().minusMinutes(15));
        ReflectionTestUtils.setField(transaction, "id", transactionId);
        ReflectionTestUtils.setField(transaction, "customerDecisionDeadlineAt", now.minusSeconds(1));

        when(transactionRepository.findExpiredAwaitingCustomerIds(now)).thenReturn(List.of(transactionId));
        when(transactionRepository.findByIdForUpdate(transactionId)).thenReturn(Optional.of(transaction));

        int transitioned = useCase.execute(now);

        assertEquals(1, transitioned);
        assertEquals(TransactionStatus.AWAITING_ANALYST, transaction.getStatus());
        assertNull(transaction.getCustomerDecisionDeadlineAt());
        verify(transactionRepository).save(transaction);
    }

    @Test
    void shouldIgnoreTransactionWhenStillInsideDeadline() {
        UUID transactionId = UUID.randomUUID();
        LocalDateTime now = LocalDateTime.now();

        Transaction transaction = new Transaction(
                UUID.randomUUID(),
                UUID.randomUUID(),
                new BigDecimal("220.00"),
                TransactionStatus.AWAITING_CUSTOMER,
                FinancialStatus.RESERVED,
                null,
                LocalDateTime.now().minusMinutes(5));
        ReflectionTestUtils.setField(transaction, "id", transactionId);
        ReflectionTestUtils.setField(transaction, "customerDecisionDeadlineAt", now.plusMinutes(2));

        when(transactionRepository.findExpiredAwaitingCustomerIds(now)).thenReturn(List.of(transactionId));
        when(transactionRepository.findByIdForUpdate(transactionId)).thenReturn(Optional.of(transaction));

        int transitioned = useCase.execute(now);

        assertEquals(0, transitioned);
        assertEquals(TransactionStatus.AWAITING_CUSTOMER, transaction.getStatus());
        verify(transactionRepository, never()).save(any(Transaction.class));
    }

    @Test
    void shouldIgnoreTransactionWhenStatusChangedByConcurrentFlow() {
        UUID transactionId = UUID.randomUUID();
        LocalDateTime now = LocalDateTime.now();

        Transaction transaction = new Transaction(
                UUID.randomUUID(),
                UUID.randomUUID(),
                new BigDecimal("80.00"),
                TransactionStatus.APPROVED,
                FinancialStatus.RESERVED,
                null,
                LocalDateTime.now());
        ReflectionTestUtils.setField(transaction, "id", transactionId);

        when(transactionRepository.findExpiredAwaitingCustomerIds(now)).thenReturn(List.of(transactionId));
        when(transactionRepository.findByIdForUpdate(transactionId)).thenReturn(Optional.of(transaction));

        int transitioned = useCase.execute(now);

        assertEquals(0, transitioned);
        assertEquals(TransactionStatus.APPROVED, transaction.getStatus());
        verify(transactionRepository, never()).save(any(Transaction.class));
    }

    @Test
    void shouldIgnoreWhenTransactionDisappearsBetweenQueries() {
        UUID transactionId = UUID.randomUUID();
        LocalDateTime now = LocalDateTime.now();

        when(transactionRepository.findExpiredAwaitingCustomerIds(now)).thenReturn(List.of(transactionId));
        when(transactionRepository.findByIdForUpdate(transactionId)).thenReturn(Optional.empty());

        int transitioned = useCase.execute(now);

        assertEquals(0, transitioned);
        verify(transactionRepository, never()).save(any(Transaction.class));
    }
}
