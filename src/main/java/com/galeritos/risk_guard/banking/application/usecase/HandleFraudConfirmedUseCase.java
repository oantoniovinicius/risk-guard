package com.galeritos.risk_guard.banking.application.usecase;

import java.util.UUID;

import org.springframework.stereotype.Service;

import com.galeritos.risk_guard.banking.domain.exception.InvalidFraudConfirmationStateException;
import com.galeritos.risk_guard.banking.domain.exception.TransactionNotFoundException;
import com.galeritos.risk_guard.banking.domain.model.Transaction;
import com.galeritos.risk_guard.banking.domain.model.enums.TransactionStatus;
import com.galeritos.risk_guard.banking.infrastructure.persistence.repository.TransactionRepository;
import com.galeritos.risk_guard.identity.domain.exception.UserNotFoundException;
import com.galeritos.risk_guard.identity.domain.model.User;
import com.galeritos.risk_guard.identity.infrastructure.persistence.repository.UserRepository;

import jakarta.transaction.Transactional;

@Service
public class HandleFraudConfirmedUseCase {
    private final TransactionRepository transactionRepository;
    private final UserRepository userRepository;
    private final FinalizeTransactionFinancialUseCase finalizeTransactionFinancialUseCase;

    public HandleFraudConfirmedUseCase(
            TransactionRepository transactionRepository,
            UserRepository userRepository,
            FinalizeTransactionFinancialUseCase finalizeTransactionFinancialUseCase) {
        this.transactionRepository = transactionRepository;
        this.userRepository = userRepository;
        this.finalizeTransactionFinancialUseCase = finalizeTransactionFinancialUseCase;
    }

    @Transactional
    public void execute(UUID transactionId) {
        Transaction transaction = transactionRepository.findByIdForUpdate(transactionId)
                .orElseThrow(() -> new TransactionNotFoundException(transactionId));

        if (transaction.getStatus() != TransactionStatus.FRAUD_CONFIRMED) {
            throw new InvalidFraudConfirmationStateException(transaction.getStatus());
        }

        finalizeTransactionFinancialUseCase.execute(transactionId); // FRAUD_CONFIRMED + RESERVED -> REVERTED

        User receiver = userRepository.findByIdForUpdate(transaction.getReceiverId())
                .orElseThrow(() -> new UserNotFoundException(transaction.getReceiverId()));

        if (!receiver.isSuspect()) {
            receiver.markAsSuspect();
            userRepository.save(receiver);
        }
    }
}
