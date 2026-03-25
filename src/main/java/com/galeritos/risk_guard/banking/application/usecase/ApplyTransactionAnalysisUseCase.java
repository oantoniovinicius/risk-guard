package com.galeritos.risk_guard.banking.application.usecase;

import java.time.LocalDateTime;

import org.springframework.stereotype.Service;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import com.galeritos.risk_guard.banking.application.event.TransactionStatusChangedEvent;
import com.galeritos.risk_guard.banking.application.port.out.BankingEventPublisher;
import com.galeritos.risk_guard.banking.domain.model.Transaction;
import com.galeritos.risk_guard.banking.domain.model.enums.TransactionStatus;
import com.galeritos.risk_guard.banking.infrastructure.persistence.repository.TransactionRepository;
import com.galeritos.risk_guard.risk.application.event.TransactionAnalyzedEvent;
import com.galeritos.risk_guard.shared.enums.RiskLevel;
import com.galeritos.risk_guard.shared.events.EventTypes;

import jakarta.transaction.Transactional;

@Service
public class ApplyTransactionAnalysisUseCase {
    private final TransactionRepository transactionRepository;
    private final FinalizeTransactionFinancialUseCase finalizeTransactionFinancialUseCase;
    private final BankingEventPublisher eventPublisher;

    public ApplyTransactionAnalysisUseCase(
            TransactionRepository transactionRepository,
            FinalizeTransactionFinancialUseCase finalizeTransactionFinancialUseCase,
            BankingEventPublisher eventPublisher) {
        this.transactionRepository = transactionRepository;
        this.finalizeTransactionFinancialUseCase = finalizeTransactionFinancialUseCase;
        this.eventPublisher = eventPublisher;
    }

    @Transactional
    public void execute(TransactionAnalyzedEvent event) {
        Transaction transaction = transactionRepository.findById(event.transactionId())
                .orElseThrow(() -> new IllegalArgumentException(
                        "Transaction not found for analysis event: " + event.transactionId()));

        if (transaction.getStatus() != TransactionStatus.CREATED
                && transaction.getStatus() != TransactionStatus.ANALYZING) {
            return;
        }

        if (transaction.getStatus() == TransactionStatus.CREATED) {
            transaction.startAnalyzing();
        }

        transaction.assignRiskLevel(event.riskLevel());
        applyDecision(transaction, event.riskLevel());
        transactionRepository.save(transaction);
        publishStatusChanged(TransactionStatusChangedEvent.from(transaction, mapStatusToEventType(transaction.getStatus())));

        if (transaction.getStatus() == TransactionStatus.APPROVED) {
            finalizeTransactionFinancialUseCase.execute(transaction.getId());
        }
    }

    private void applyDecision(Transaction transaction, RiskLevel riskLevel) {
        switch (riskLevel) {
            case LOW -> transaction.approve();
            case MEDIUM -> transaction.awaitAnalyst();
            case HIGH -> transaction.awaitCustomer(LocalDateTime.now().plusMinutes(10));
        }
    }

    private String mapStatusToEventType(TransactionStatus status) {
        return switch (status) {
            case APPROVED -> EventTypes.TRANSACTION_APPROVED;
            case AWAITING_CUSTOMER -> EventTypes.TRANSACTION_AWAITING_CUSTOMER;
            case AWAITING_ANALYST -> EventTypes.TRANSACTION_AWAITING_ANALYST;
            default -> throw new IllegalStateException("Unsupported status for analysis event publishing: " + status);
        };
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
