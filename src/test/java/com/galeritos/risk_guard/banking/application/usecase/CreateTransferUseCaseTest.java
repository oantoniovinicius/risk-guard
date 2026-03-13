package com.galeritos.risk_guard.banking.application.usecase;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.util.Optional;
import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import com.galeritos.risk_guard.banking.application.event.TransactionCreatedEvent;
import com.galeritos.risk_guard.banking.application.port.out.EventPublisher;
import com.galeritos.risk_guard.banking.application.usecase.dto.CreateTransferCommand;
import com.galeritos.risk_guard.banking.domain.exception.AccountNotFoundException;
import com.galeritos.risk_guard.banking.domain.exception.InsufficientBalanceException;
import com.galeritos.risk_guard.banking.domain.exception.InvalidTransferException;
import com.galeritos.risk_guard.banking.domain.model.Account;
import com.galeritos.risk_guard.banking.domain.model.Transaction;
import com.galeritos.risk_guard.banking.domain.model.enums.FinancialStatus;
import com.galeritos.risk_guard.banking.domain.model.enums.TransactionStatus;
import com.galeritos.risk_guard.banking.infrastructure.persistence.repository.AccountRepository;
import com.galeritos.risk_guard.banking.infrastructure.persistence.repository.TransactionRepository;

@ExtendWith(MockitoExtension.class)
class CreateTransferUseCaseTest {

    @Mock
    private AccountRepository accountRepository;

    @Mock
    private TransactionRepository transactionRepository;

    @Mock
    private EventPublisher eventPublisher;

    @InjectMocks
    private CreateTransferUseCase useCase;

    @Test
    void shouldReserveBalanceCreateTransactionAndPublishEvent() {
        UUID senderId = UUID.randomUUID();
        UUID receiverId = UUID.randomUUID();
        UUID transactionId = UUID.randomUUID();
        CreateTransferCommand command = new CreateTransferCommand(senderId, receiverId, new BigDecimal("100.00"));
        Account senderAccount = new Account(UUID.randomUUID(), senderId, new BigDecimal("250.00"), new BigDecimal("25.00"));

        when(accountRepository.findByUserIdForUpdate(senderId)).thenReturn(Optional.of(senderAccount));
        when(transactionRepository.save(any(Transaction.class))).thenAnswer(invocation -> {
            Transaction transaction = invocation.getArgument(0);
            ReflectionTestUtils.setField(transaction, "id", transactionId);
            return transaction;
        });

        Transaction transaction = useCase.execute(command);

        assertNotNull(transaction.getCreatedAt());
        assertEquals(transactionId, transaction.getId());
        assertEquals(senderId, transaction.getSenderId());
        assertEquals(receiverId, transaction.getReceiverId());
        assertEquals(new BigDecimal("100.00"), transaction.getAmount());
        assertEquals(TransactionStatus.CREATED, transaction.getStatus());
        assertEquals(FinancialStatus.RESERVED, transaction.getFinancialStatus());
        assertEquals(new BigDecimal("150.00"), senderAccount.getBalance());
        assertEquals(new BigDecimal("125.00"), senderAccount.getReservedBalance());

        ArgumentCaptor<Account> accountCaptor = ArgumentCaptor.forClass(Account.class);
        verify(accountRepository).save(accountCaptor.capture());
        assertEquals(new BigDecimal("150.00"), accountCaptor.getValue().getBalance());
        assertEquals(new BigDecimal("125.00"), accountCaptor.getValue().getReservedBalance());

        ArgumentCaptor<TransactionCreatedEvent> eventCaptor = ArgumentCaptor.forClass(TransactionCreatedEvent.class);
        verify(eventPublisher).publishTransactionCreated(eventCaptor.capture());
        TransactionCreatedEvent publishedEvent = eventCaptor.getValue();
        assertEquals(transactionId, publishedEvent.transactionId());
        assertEquals(senderId, publishedEvent.senderId());
        assertEquals(receiverId, publishedEvent.receiverId());
        assertEquals(new BigDecimal("100.00"), publishedEvent.amount());
        assertEquals(TransactionStatus.CREATED, publishedEvent.status());
        assertEquals(FinancialStatus.RESERVED, publishedEvent.financialStatus());
        assertNotNull(publishedEvent.createdAt());
    }

    @Test
    void shouldRejectTransferToSameUser() {
        UUID userId = UUID.randomUUID();
        CreateTransferCommand command = new CreateTransferCommand(userId, userId, new BigDecimal("50.00"));

        assertThrows(InvalidTransferException.class, () -> useCase.execute(command));

        verify(accountRepository, never()).findByUserIdForUpdate(any());
        verify(accountRepository, never()).save(any(Account.class));
        verify(transactionRepository, never()).save(any(Transaction.class));
        verify(eventPublisher, never()).publishTransactionCreated(any());
    }

    @Test
    void shouldRejectTransferWithNonPositiveAmount() {
        UUID senderId = UUID.randomUUID();
        UUID receiverId = UUID.randomUUID();
        Account senderAccount = new Account(UUID.randomUUID(), senderId, new BigDecimal("250.00"), BigDecimal.ZERO);
        CreateTransferCommand command = new CreateTransferCommand(senderId, receiverId, BigDecimal.ZERO);

        when(accountRepository.findByUserIdForUpdate(senderId)).thenReturn(Optional.of(senderAccount));

        assertThrows(InvalidTransferException.class, () -> useCase.execute(command));

        verify(accountRepository, never()).save(any(Account.class));
        verify(transactionRepository, never()).save(any(Transaction.class));
        verify(eventPublisher, never()).publishTransactionCreated(any());
    }

    @Test
    void shouldThrowWhenSenderAccountDoesNotExist() {
        UUID senderId = UUID.randomUUID();
        UUID receiverId = UUID.randomUUID();
        CreateTransferCommand command = new CreateTransferCommand(senderId, receiverId, new BigDecimal("10.00"));

        when(accountRepository.findByUserIdForUpdate(senderId)).thenReturn(Optional.empty());

        assertThrows(AccountNotFoundException.class, () -> useCase.execute(command));

        verify(accountRepository, never()).save(any(Account.class));
        verify(transactionRepository, never()).save(any(Transaction.class));
        verify(eventPublisher, never()).publishTransactionCreated(any());
    }

    @Test
    void shouldThrowWhenSenderHasInsufficientBalance() {
        UUID senderId = UUID.randomUUID();
        UUID receiverId = UUID.randomUUID();
        Account senderAccount = new Account(UUID.randomUUID(), senderId, new BigDecimal("40.00"), new BigDecimal("5.00"));
        CreateTransferCommand command = new CreateTransferCommand(senderId, receiverId, new BigDecimal("50.00"));

        when(accountRepository.findByUserIdForUpdate(senderId)).thenReturn(Optional.of(senderAccount));

        assertThrows(InsufficientBalanceException.class, () -> useCase.execute(command));

        assertEquals(new BigDecimal("40.00"), senderAccount.getBalance());
        assertEquals(new BigDecimal("5.00"), senderAccount.getReservedBalance());
        verify(accountRepository, never()).save(any(Account.class));
        verify(transactionRepository, never()).save(any(Transaction.class));
        verify(eventPublisher, never()).publishTransactionCreated(any());
    }
}
