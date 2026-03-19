package com.galeritos.risk_guard.banking.application.usecase;

import java.util.UUID;

import org.springframework.stereotype.Service;

import com.galeritos.risk_guard.banking.domain.exception.AccountNotFoundException;
import com.galeritos.risk_guard.banking.domain.exception.InvalidTransactionFinancialFinalizationException;
import com.galeritos.risk_guard.banking.domain.model.Account;
import com.galeritos.risk_guard.banking.domain.model.Transaction;
import com.galeritos.risk_guard.banking.domain.model.enums.FinancialStatus;
import com.galeritos.risk_guard.banking.domain.model.enums.TransactionStatus;
import com.galeritos.risk_guard.banking.domain.exception.TransactionNotFoundException;
import com.galeritos.risk_guard.banking.infrastructure.persistence.repository.AccountRepository;
import com.galeritos.risk_guard.banking.infrastructure.persistence.repository.TransactionRepository;

import jakarta.transaction.Transactional;

@Service
public class FinalizeTransactionFinancialUseCase {
    private final TransactionRepository transactionRepository;
    private final AccountRepository accountRepository;

    public FinalizeTransactionFinancialUseCase(
            TransactionRepository transactionRepository,
            AccountRepository accountRepository) {
        this.transactionRepository = transactionRepository;
        this.accountRepository = accountRepository;
    }

    @Transactional
    public void execute(UUID transactionId) {
        Transaction transaction = transactionRepository.findByIdForUpdate(transactionId)
                .orElseThrow(() -> new TransactionNotFoundException(transactionId));

        if (transaction.getFinancialStatus() != FinancialStatus.RESERVED) {
            return;
        }

        Account sender = accountRepository.findByUserIdForUpdate(transaction.getSenderId())
                .orElseThrow(() -> new AccountNotFoundException("Sender account not found."));

        if (transaction.getStatus() == TransactionStatus.APPROVED) {
            Account receiver = accountRepository.findByUserIdForUpdate(transaction.getReceiverId())
                    .orElseThrow(() -> new AccountNotFoundException("Receiver account not found."));

            sender.consumeReserved(transaction.getAmount());
            receiver.credit(transaction.getAmount());
            transaction.settle();

            accountRepository.save(sender);
            accountRepository.save(receiver);
            transactionRepository.save(transaction);
            return;
        }

        if (transaction.getStatus() == TransactionStatus.DENIED
                || transaction.getStatus() == TransactionStatus.FRAUD_CONFIRMED) {
            sender.releaseReserved(transaction.getAmount());
            transaction.revert();

            accountRepository.save(sender);
            transactionRepository.save(transaction);
            return;
        }

        throw new InvalidTransactionFinancialFinalizationException(transaction.getStatus());
    }
}
