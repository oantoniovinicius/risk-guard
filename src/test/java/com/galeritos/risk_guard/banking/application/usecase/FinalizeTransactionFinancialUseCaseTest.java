package com.galeritos.risk_guard.banking.application.usecase;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
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

import com.galeritos.risk_guard.banking.domain.exception.AccountNotFoundException;
import com.galeritos.risk_guard.banking.domain.exception.InsufficientReservedBalanceException;
import com.galeritos.risk_guard.banking.domain.exception.InvalidTransactionFinancialFinalizationException;
import com.galeritos.risk_guard.banking.domain.exception.TransactionNotFoundException;
import com.galeritos.risk_guard.banking.domain.model.Account;
import com.galeritos.risk_guard.banking.domain.model.Transaction;
import com.galeritos.risk_guard.banking.domain.model.enums.FinancialStatus;
import com.galeritos.risk_guard.banking.domain.model.enums.TransactionStatus;
import com.galeritos.risk_guard.banking.infrastructure.persistence.repository.AccountRepository;
import com.galeritos.risk_guard.banking.infrastructure.persistence.repository.TransactionRepository;

@ExtendWith(MockitoExtension.class)
class FinalizeTransactionFinancialUseCaseTest {

    @Mock
    private TransactionRepository transactionRepository;

    @Mock
    private AccountRepository accountRepository;

    @InjectMocks
    private FinalizeTransactionFinancialUseCase useCase;

    @Test
    void shouldSettleApprovedTransaction() {
        UUID senderId = UUID.randomUUID();
        UUID receiverId = UUID.randomUUID();
        UUID transactionId = UUID.randomUUID();

        Transaction transaction = new Transaction(
                senderId,
                receiverId,
                new BigDecimal("100.00"),
                TransactionStatus.APPROVED,
                FinancialStatus.RESERVED,
                null,
                LocalDateTime.now());
        ReflectionTestUtils.setField(transaction, "id", transactionId);

        Account sender = new Account(UUID.randomUUID(), senderId, new BigDecimal("200.00"), new BigDecimal("100.00"));
        Account receiver = new Account(UUID.randomUUID(), receiverId, new BigDecimal("30.00"), BigDecimal.ZERO);

        when(transactionRepository.findByIdForUpdate(transactionId)).thenReturn(Optional.of(transaction));
        when(accountRepository.findByUserIdForUpdate(senderId)).thenReturn(Optional.of(sender));
        when(accountRepository.findByUserIdForUpdate(receiverId)).thenReturn(Optional.of(receiver));

        useCase.execute(transactionId);

        assertEquals(0, sender.getReservedBalance().compareTo(BigDecimal.ZERO));
        assertEquals(0, receiver.getBalance().compareTo(new BigDecimal("130.00")));
        assertEquals(FinancialStatus.SETTLED, transaction.getFinancialStatus());
        verify(accountRepository).save(sender);
        verify(accountRepository).save(receiver);
        verify(transactionRepository).save(transaction);
    }

    @Test
    void shouldRevertDeniedTransaction() {
        UUID senderId = UUID.randomUUID();
        UUID receiverId = UUID.randomUUID();
        UUID transactionId = UUID.randomUUID();

        Transaction transaction = new Transaction(
                senderId,
                receiverId,
                new BigDecimal("70.00"),
                TransactionStatus.DENIED,
                FinancialStatus.RESERVED,
                null,
                LocalDateTime.now());
        ReflectionTestUtils.setField(transaction, "id", transactionId);

        Account sender = new Account(UUID.randomUUID(), senderId, new BigDecimal("40.00"), new BigDecimal("70.00"));

        when(transactionRepository.findByIdForUpdate(transactionId)).thenReturn(Optional.of(transaction));
        when(accountRepository.findByUserIdForUpdate(senderId)).thenReturn(Optional.of(sender));

        useCase.execute(transactionId);

        assertEquals(0, sender.getReservedBalance().compareTo(BigDecimal.ZERO));
        assertEquals(0, sender.getBalance().compareTo(new BigDecimal("110.00")));
        assertEquals(FinancialStatus.REVERTED, transaction.getFinancialStatus());
        verify(accountRepository).save(sender);
        verify(transactionRepository).save(transaction);
    }

    @Test
    void shouldRevertFraudConfirmedTransaction() {
        UUID senderId = UUID.randomUUID();
        UUID receiverId = UUID.randomUUID();
        UUID transactionId = UUID.randomUUID();

        Transaction transaction = new Transaction(
                senderId,
                receiverId,
                new BigDecimal("50.00"),
                TransactionStatus.FRAUD_CONFIRMED,
                FinancialStatus.RESERVED,
                null,
                LocalDateTime.now());
        ReflectionTestUtils.setField(transaction, "id", transactionId);

        Account sender = new Account(UUID.randomUUID(), senderId, new BigDecimal("20.00"), new BigDecimal("50.00"));

        when(transactionRepository.findByIdForUpdate(transactionId)).thenReturn(Optional.of(transaction));
        when(accountRepository.findByUserIdForUpdate(senderId)).thenReturn(Optional.of(sender));

        useCase.execute(transactionId);

        assertEquals(0, sender.getReservedBalance().compareTo(BigDecimal.ZERO));
        assertEquals(0, sender.getBalance().compareTo(new BigDecimal("70.00")));
        assertEquals(FinancialStatus.REVERTED, transaction.getFinancialStatus());
        verify(accountRepository).save(sender);
        verify(transactionRepository).save(transaction);
    }

    @Test
    void shouldIgnoreWhenFinancialStatusAlreadyFinalized() {
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

        useCase.execute(transactionId);

        verify(accountRepository, never()).findByUserIdForUpdate(org.mockito.ArgumentMatchers.any(UUID.class));
        verify(accountRepository, never()).save(org.mockito.ArgumentMatchers.any(Account.class));
        verify(transactionRepository, never()).save(transaction);
    }

    @Test
    void shouldThrowWhenTransactionDoesNotExist() {
        UUID transactionId = UUID.randomUUID();

        when(transactionRepository.findByIdForUpdate(transactionId)).thenReturn(Optional.empty());

        assertThrows(TransactionNotFoundException.class, () -> useCase.execute(transactionId));
        verify(accountRepository, never()).findByUserIdForUpdate(org.mockito.ArgumentMatchers.any(UUID.class));
        verify(transactionRepository, never()).save(org.mockito.ArgumentMatchers.any(Transaction.class));
    }

    @Test
    void shouldThrowWhenSenderAccountDoesNotExist() {
        UUID transactionId = UUID.randomUUID();
        Transaction transaction = new Transaction(
                UUID.randomUUID(),
                UUID.randomUUID(),
                new BigDecimal("100.00"),
                TransactionStatus.DENIED,
                FinancialStatus.RESERVED,
                null,
                LocalDateTime.now());
        ReflectionTestUtils.setField(transaction, "id", transactionId);

        when(transactionRepository.findByIdForUpdate(transactionId)).thenReturn(Optional.of(transaction));
        when(accountRepository.findByUserIdForUpdate(transaction.getSenderId())).thenReturn(Optional.empty());

        assertThrows(AccountNotFoundException.class, () -> useCase.execute(transactionId));
        verify(transactionRepository, never()).save(transaction);
    }

    @Test
    void shouldThrowWhenReceiverAccountDoesNotExistForApprovedTransaction() {
        UUID senderId = UUID.randomUUID();
        UUID receiverId = UUID.randomUUID();
        UUID transactionId = UUID.randomUUID();
        Transaction transaction = new Transaction(
                senderId,
                receiverId,
                new BigDecimal("80.00"),
                TransactionStatus.APPROVED,
                FinancialStatus.RESERVED,
                null,
                LocalDateTime.now());
        ReflectionTestUtils.setField(transaction, "id", transactionId);
        Account sender = new Account(UUID.randomUUID(), senderId, BigDecimal.ZERO, new BigDecimal("80.00"));

        when(transactionRepository.findByIdForUpdate(transactionId)).thenReturn(Optional.of(transaction));
        when(accountRepository.findByUserIdForUpdate(senderId)).thenReturn(Optional.of(sender));
        when(accountRepository.findByUserIdForUpdate(receiverId)).thenReturn(Optional.empty());

        assertThrows(AccountNotFoundException.class, () -> useCase.execute(transactionId));
        verify(transactionRepository, never()).save(transaction);
    }

    @Test
    void shouldThrowWhenReservedBalanceIsInsufficient() {
        UUID senderId = UUID.randomUUID();
        UUID receiverId = UUID.randomUUID();
        UUID transactionId = UUID.randomUUID();
        Transaction transaction = new Transaction(
                senderId,
                receiverId,
                new BigDecimal("100.00"),
                TransactionStatus.APPROVED,
                FinancialStatus.RESERVED,
                null,
                LocalDateTime.now());
        ReflectionTestUtils.setField(transaction, "id", transactionId);

        Account sender = new Account(UUID.randomUUID(), senderId, new BigDecimal("200.00"), new BigDecimal("20.00"));
        Account receiver = new Account(UUID.randomUUID(), receiverId, BigDecimal.ZERO, BigDecimal.ZERO);

        when(transactionRepository.findByIdForUpdate(transactionId)).thenReturn(Optional.of(transaction));
        when(accountRepository.findByUserIdForUpdate(senderId)).thenReturn(Optional.of(sender));
        when(accountRepository.findByUserIdForUpdate(receiverId)).thenReturn(Optional.of(receiver));

        assertThrows(InsufficientReservedBalanceException.class, () -> useCase.execute(transactionId));
        verify(transactionRepository, never()).save(transaction);
    }

    @Test
    void shouldThrowWhenStatusIsNotFinalizable() {
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
        when(accountRepository.findByUserIdForUpdate(transaction.getSenderId()))
                .thenReturn(Optional.of(new Account(
                        UUID.randomUUID(),
                        transaction.getSenderId(),
                        BigDecimal.ZERO,
                        new BigDecimal("100.00"))));

        assertThrows(InvalidTransactionFinancialFinalizationException.class, () -> useCase.execute(transactionId));
        verify(transactionRepository, never()).save(transaction);
    }
}
