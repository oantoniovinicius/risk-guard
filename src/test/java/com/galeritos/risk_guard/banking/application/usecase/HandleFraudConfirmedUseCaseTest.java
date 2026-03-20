package com.galeritos.risk_guard.banking.application.usecase;

import static org.junit.jupiter.api.Assertions.assertEquals;
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
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import com.galeritos.risk_guard.banking.domain.exception.InvalidFraudConfirmationStateException;
import com.galeritos.risk_guard.banking.domain.exception.TransactionNotFoundException;
import com.galeritos.risk_guard.banking.domain.exception.AccountNotFoundException;
import com.galeritos.risk_guard.banking.domain.model.Transaction;
import com.galeritos.risk_guard.banking.domain.model.enums.FinancialStatus;
import com.galeritos.risk_guard.banking.domain.model.enums.TransactionStatus;
import com.galeritos.risk_guard.banking.infrastructure.persistence.repository.TransactionRepository;
import com.galeritos.risk_guard.identity.domain.exception.UserNotFoundException;
import com.galeritos.risk_guard.identity.domain.model.User;
import com.galeritos.risk_guard.identity.domain.model.enums.Role;
import com.galeritos.risk_guard.identity.domain.model.enums.UserStatus;
import com.galeritos.risk_guard.identity.infrastructure.persistence.repository.UserRepository;

@ExtendWith(MockitoExtension.class)
class HandleFraudConfirmedUseCaseTest {

    @Mock
    private TransactionRepository transactionRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private FinalizeTransactionFinancialUseCase finalizeTransactionFinancialUseCase;

    @InjectMocks
    private HandleFraudConfirmedUseCase useCase;

    @Test
    void shouldMarkReceiverAsSuspectAndFinalizeFinancialWhenFraudIsConfirmed() {
        UUID transactionId = UUID.randomUUID();
        UUID receiverId = UUID.randomUUID();

        Transaction transaction = new Transaction(
                UUID.randomUUID(),
                receiverId,
                new BigDecimal("100.00"),
                TransactionStatus.FRAUD_CONFIRMED,
                FinancialStatus.RESERVED,
                null,
                LocalDateTime.now());
        ReflectionTestUtils.setField(transaction, "id", transactionId);

        User receiver = new User(
                receiverId,
                "Receiver",
                "receiver+" + receiverId + "@example.com",
                "12345678909",
                Role.USER,
                UserStatus.ACTIVE);

        when(transactionRepository.findByIdForUpdate(transactionId)).thenReturn(Optional.of(transaction));
        when(userRepository.findByIdForUpdate(receiverId)).thenReturn(Optional.of(receiver));

        useCase.execute(transactionId);

        assertEquals(UserStatus.ACTIVE, receiver.getStatus());
        assertTrue(receiver.isSuspect());
        verify(finalizeTransactionFinancialUseCase).execute(transactionId);
        verify(userRepository).save(receiver);
    }

    @Test
    void shouldBeIdempotentWhenReceiverAlreadySuspect() {
        UUID transactionId = UUID.randomUUID();
        UUID receiverId = UUID.randomUUID();

        Transaction transaction = new Transaction(
                UUID.randomUUID(),
                receiverId,
                new BigDecimal("100.00"),
                TransactionStatus.FRAUD_CONFIRMED,
                FinancialStatus.REVERTED,
                null,
                LocalDateTime.now());
        ReflectionTestUtils.setField(transaction, "id", transactionId);

        User receiver = new User(
                receiverId,
                "Receiver",
                "receiver+" + receiverId + "@example.com",
                "22345678909",
                Role.USER,
                UserStatus.ACTIVE);
        receiver.markAsSuspect();

        when(transactionRepository.findByIdForUpdate(transactionId)).thenReturn(Optional.of(transaction));
        when(userRepository.findByIdForUpdate(receiverId)).thenReturn(Optional.of(receiver));

        useCase.execute(transactionId);

        assertEquals(UserStatus.ACTIVE, receiver.getStatus());
        assertTrue(receiver.isSuspect());
        verify(finalizeTransactionFinancialUseCase).execute(transactionId);
        verify(userRepository, never()).save(receiver);
    }

    @Test
    void shouldThrowWhenTransactionDoesNotExist() {
        UUID transactionId = UUID.randomUUID();
        when(transactionRepository.findByIdForUpdate(transactionId)).thenReturn(Optional.empty());

        assertThrows(TransactionNotFoundException.class, () -> useCase.execute(transactionId));
        verify(finalizeTransactionFinancialUseCase, never()).execute(transactionId);
    }

    @Test
    void shouldThrowWhenTransactionIsNotFraudConfirmed() {
        UUID transactionId = UUID.randomUUID();
        Transaction transaction = new Transaction(
                UUID.randomUUID(),
                UUID.randomUUID(),
                new BigDecimal("90.00"),
                TransactionStatus.AWAITING_CUSTOMER,
                FinancialStatus.RESERVED,
                null,
                LocalDateTime.now());
        ReflectionTestUtils.setField(transaction, "id", transactionId);

        when(transactionRepository.findByIdForUpdate(transactionId)).thenReturn(Optional.of(transaction));

        assertThrows(InvalidFraudConfirmationStateException.class, () -> useCase.execute(transactionId));
        verify(finalizeTransactionFinancialUseCase, never()).execute(transactionId);
        verify(userRepository, never()).findByIdForUpdate(any(UUID.class));
        verify(userRepository, never()).save(any(User.class));
    }

    @Test
    void shouldThrowWhenReceiverDoesNotExist() {
        UUID transactionId = UUID.randomUUID();
        UUID receiverId = UUID.randomUUID();
        Transaction transaction = new Transaction(
                UUID.randomUUID(),
                receiverId,
                new BigDecimal("110.00"),
                TransactionStatus.FRAUD_CONFIRMED,
                FinancialStatus.RESERVED,
                null,
                LocalDateTime.now());
        ReflectionTestUtils.setField(transaction, "id", transactionId);

        when(transactionRepository.findByIdForUpdate(transactionId)).thenReturn(Optional.of(transaction));
        when(userRepository.findByIdForUpdate(receiverId)).thenReturn(Optional.empty());

        assertThrows(UserNotFoundException.class, () -> useCase.execute(transactionId));
        verify(finalizeTransactionFinancialUseCase).execute(transactionId);
    }

    @Test
    void shouldNotSaveUserWhenFinancialFinalizationFails() {
        UUID transactionId = UUID.randomUUID();
        UUID receiverId = UUID.randomUUID();
        Transaction transaction = new Transaction(
                UUID.randomUUID(),
                receiverId,
                new BigDecimal("110.00"),
                TransactionStatus.FRAUD_CONFIRMED,
                FinancialStatus.RESERVED,
                null,
                LocalDateTime.now());
        ReflectionTestUtils.setField(transaction, "id", transactionId);

        when(transactionRepository.findByIdForUpdate(transactionId)).thenReturn(Optional.of(transaction));
        org.mockito.Mockito.doThrow(new AccountNotFoundException("Sender account not found."))
                .when(finalizeTransactionFinancialUseCase)
                .execute(transactionId);

        assertThrows(AccountNotFoundException.class, () -> useCase.execute(transactionId));
        verify(userRepository, never()).findByIdForUpdate(any(UUID.class));
        verify(userRepository, never()).save(any(User.class));
    }
}
