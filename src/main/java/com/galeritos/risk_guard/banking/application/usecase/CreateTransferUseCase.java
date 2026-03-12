package com.galeritos.risk_guard.banking.application.usecase;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

import org.springframework.stereotype.Service;

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

    public CreateTransferUseCase(AccountRepository accountRepository, TransactionRepository transactionRepository) {
        this.accountRepository = accountRepository;
        this.transactionRepository = transactionRepository;
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
                UUID.randomUUID(),
                command.senderId(),
                command.receiverId(),
                command.amount(),
                TransactionStatus.CREATED,
                FinancialStatus.RESERVED,
                null,
                LocalDateTime.now());

        transactionRepository.save(transaction);

        return transaction;
    }
}
