package com.galeritos.risk_guard.banking.application.usecase;

import java.util.UUID;

import org.springframework.stereotype.Service;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import com.galeritos.risk_guard.banking.application.event.TransactionStatusChangedEvent;
import com.galeritos.risk_guard.banking.application.port.out.BankingEventPublisher;
import com.galeritos.risk_guard.banking.domain.exception.InvalidDisputeStateException;
import com.galeritos.risk_guard.banking.domain.exception.TransactionNotFoundException;
import com.galeritos.risk_guard.banking.domain.model.Transaction;
import com.galeritos.risk_guard.banking.domain.model.enums.TransactionStatus;
import com.galeritos.risk_guard.banking.infrastructure.persistence.repository.TransactionRepository;
import com.galeritos.risk_guard.shared.events.EventTypes;

import jakarta.transaction.Transactional;

@Service
public class OpenDisputeUseCase {
    private final TransactionRepository transactionRepository;
    private final BankingEventPublisher eventPublisher;

    public OpenDisputeUseCase(TransactionRepository transactionRepository, BankingEventPublisher eventPublisher) {
        this.transactionRepository = transactionRepository;
        this.eventPublisher = eventPublisher;
    }

    @Transactional
    public void execute(UUID transactionId) {
        Transaction transaction = transactionRepository.findByIdForUpdate(transactionId)
                .orElseThrow(() -> new TransactionNotFoundException(transactionId));

        if (transaction.getStatus() != TransactionStatus.APPROVED) {
            throw new InvalidDisputeStateException(transaction.getStatus());
        }

        transaction.dispute();
        transactionRepository.save(transaction);

        TransactionStatusChangedEvent event = TransactionStatusChangedEvent.from(
                transaction, EventTypes.TRANSACTION_DISPUTED);

        TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
            @Override
            public void afterCommit() {
                eventPublisher.publishTransactionStatusChanged(event);
            }
        });
    }
}
