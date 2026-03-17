package com.galeritos.risk_guard.banking.application.usecase;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import org.springframework.stereotype.Service;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import com.galeritos.risk_guard.banking.application.event.TransactionCreatedEvent;
import com.galeritos.risk_guard.banking.application.port.out.BankingEventPublisher;
import com.galeritos.risk_guard.banking.application.usecase.dto.CreateTransferCommand;
import com.galeritos.risk_guard.banking.domain.exception.AccountNotFoundException;
import com.galeritos.risk_guard.banking.domain.exception.InvalidTransferException;
import com.galeritos.risk_guard.banking.domain.model.Account;
import com.galeritos.risk_guard.banking.domain.model.Transaction;
import com.galeritos.risk_guard.banking.domain.model.enums.FinancialStatus;
import com.galeritos.risk_guard.banking.domain.model.enums.TransactionStatus;
import com.galeritos.risk_guard.banking.infrastructure.persistence.repository.AccountRepository;
import com.galeritos.risk_guard.banking.infrastructure.persistence.repository.TransactionRepository;

import jakarta.transaction.Transactional;

@Service
public class CreateTransferUseCase {
    private final AccountRepository accountRepository;
    private final TransactionRepository transactionRepository;
    private final BankingEventPublisher eventPublisher;

    public CreateTransferUseCase(
            AccountRepository accountRepository,
            TransactionRepository transactionRepository,
            BankingEventPublisher eventPublisher) {
        this.accountRepository = accountRepository;
        this.transactionRepository = transactionRepository;
        this.eventPublisher = eventPublisher;
    }

    @Transactional
    public Transaction execute(CreateTransferCommand command) {
        if (command.senderId().equals(command.receiverId())) {
            throw new InvalidTransferException("Sender and receiver cannot be the same.");
        }

        Account sender = accountRepository.findByUserIdForUpdate(command.senderId())
                .orElseThrow(() -> new AccountNotFoundException("Sender account not found."));

        if (command.amount().compareTo(BigDecimal.ZERO) <= 0) {
            throw new InvalidTransferException("Transfer amount must be positive");
        }

        sender.reserve(command.amount());

        accountRepository.save(sender);

        Transaction transaction = new Transaction(
                command.senderId(),
                command.receiverId(),
                command.amount(),
                TransactionStatus.CREATED,
                FinancialStatus.RESERVED,
                null,
                LocalDateTime.now());

        transaction = transactionRepository.save(transaction);
        publishTransactionCreated(TransactionCreatedEvent.from(transaction));

        return transaction;
    }

    private void publishTransactionCreated(TransactionCreatedEvent event) {
        if (!TransactionSynchronizationManager.isSynchronizationActive()) {
            eventPublisher.publishTransactionCreated(event);
            return;
        }

        TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
            @Override
            public void afterCommit() {
                eventPublisher.publishTransactionCreated(event);
            }
        });
    }
}
