package com.galeritos.risk_guard.banking.application.usecase;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

import org.springframework.stereotype.Service;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import com.galeritos.risk_guard.banking.application.event.TransactionStatusChangedEvent;
import com.galeritos.risk_guard.banking.application.port.out.BankingEventPublisher;
import com.galeritos.risk_guard.banking.domain.model.enums.TransactionStatus;
import com.galeritos.risk_guard.banking.infrastructure.persistence.repository.TransactionRepository;
import com.galeritos.risk_guard.shared.events.EventTypes;

import jakarta.transaction.Transactional;

@Service
public class HandleCustomerConfirmationTimeoutUseCase {
    private final TransactionRepository transactionRepository;
    private final BankingEventPublisher eventPublisher;

    public HandleCustomerConfirmationTimeoutUseCase(
            TransactionRepository transactionRepository,
            BankingEventPublisher eventPublisher) {
        this.transactionRepository = transactionRepository;
        this.eventPublisher = eventPublisher;
    }

    @Transactional
    public int execute(LocalDateTime now) {
        List<UUID> expiredTransactionIds = transactionRepository.findExpiredAwaitingCustomerIds(now);
        int transitioned = 0;

        for (UUID transactionId : expiredTransactionIds) {
            if (moveToAwaitingAnalystWhenExpired(transactionId, now)) {
                transitioned++;
            }
        }

        return transitioned;
    }

    private boolean moveToAwaitingAnalystWhenExpired(UUID transactionId, LocalDateTime now) {
        return transactionRepository.findByIdForUpdate(transactionId)
                .map(transaction -> {
                    if (transaction.getStatus() != TransactionStatus.AWAITING_CUSTOMER) {
                        return false;
                    }

                    if (transaction.getCustomerDecisionDeadlineAt() == null
                            || transaction.getCustomerDecisionDeadlineAt().isAfter(now)) {
                        return false;
                    }

                    transaction.awaitAnalyst();
                    transactionRepository.save(transaction);
                    publishStatusChanged(TransactionStatusChangedEvent.from(
                            transaction,
                            EventTypes.TRANSACTION_AWAITING_ANALYST));
                    return true;
                })
                .orElse(false);
    }

    private void publishStatusChanged(TransactionStatusChangedEvent event) {
        if (!TransactionSynchronizationManager.isSynchronizationActive()) {
            eventPublisher.publishTransactionStatusChanged(event);
            return;
        }

        TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
            @Override
            public void afterCommit() {
                eventPublisher.publishTransactionStatusChanged(event);
            }
        });
    }
}
