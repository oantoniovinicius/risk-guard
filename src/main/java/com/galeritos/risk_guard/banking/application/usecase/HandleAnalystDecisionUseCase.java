package com.galeritos.risk_guard.banking.application.usecase;

import java.time.LocalDateTime;
import java.util.UUID;

import org.springframework.stereotype.Service;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import com.galeritos.risk_guard.banking.application.event.TransactionStatusChangedEvent;
import com.galeritos.risk_guard.banking.application.port.out.BankingEventPublisher;
import com.galeritos.risk_guard.banking.domain.exception.AnalystConflictOfInterestException;
import com.galeritos.risk_guard.banking.domain.exception.InvalidAnalystDecisionStateException;
import com.galeritos.risk_guard.banking.domain.exception.TransactionNotFoundException;
import com.galeritos.risk_guard.banking.domain.model.Transaction;
import com.galeritos.risk_guard.banking.domain.model.TransactionDecisionHistory;
import com.galeritos.risk_guard.banking.domain.model.enums.AnalystDecision;
import com.galeritos.risk_guard.banking.domain.model.enums.TransactionStatus;
import com.galeritos.risk_guard.banking.infrastructure.persistence.repository.TransactionDecisionHistoryRepository;
import com.galeritos.risk_guard.banking.infrastructure.persistence.repository.TransactionRepository;
import com.galeritos.risk_guard.identity.application.security.CurrentUserProvider;
import com.galeritos.risk_guard.shared.events.EventTypes;

import jakarta.transaction.Transactional;

@Service
public class HandleAnalystDecisionUseCase {
    private final TransactionRepository transactionRepository;
    private final TransactionDecisionHistoryRepository decisionHistoryRepository;
    private final FinalizeTransactionFinancialUseCase finalizeTransactionFinancialUseCase;
    private final HandleFraudConfirmedUseCase handleFraudConfirmedUseCase;
    private final BankingEventPublisher eventPublisher;
    private final CurrentUserProvider currentUserProvider;

    public HandleAnalystDecisionUseCase(
            TransactionRepository transactionRepository,
            TransactionDecisionHistoryRepository decisionHistoryRepository,
            FinalizeTransactionFinancialUseCase finalizeTransactionFinancialUseCase,
            HandleFraudConfirmedUseCase handleFraudConfirmedUseCase,
            BankingEventPublisher eventPublisher,
            CurrentUserProvider currentUserProvider) {
        this.transactionRepository = transactionRepository;
        this.decisionHistoryRepository = decisionHistoryRepository;
        this.finalizeTransactionFinancialUseCase = finalizeTransactionFinancialUseCase;
        this.handleFraudConfirmedUseCase = handleFraudConfirmedUseCase;
        this.eventPublisher = eventPublisher;
        this.currentUserProvider = currentUserProvider;
    }

    @Transactional
    public void execute(UUID transactionId, AnalystDecision decision, String reason) {
        Transaction transaction = transactionRepository.findByIdForUpdate(transactionId)
                .orElseThrow(() -> new TransactionNotFoundException(transactionId));

        UUID analystId = currentUserProvider.getAuthenticatedUser().userId();
        if (transaction.getReceiverId().equals(analystId)) {
            throw new AnalystConflictOfInterestException();
        }

        if (transaction.getStatus() == TransactionStatus.AWAITING_ANALYST) {
            TransactionStatus fromStatus = transaction.getStatus();
            applyFromAwaitingAnalyst(transaction, decision);
            TransactionStatus toStatus = transaction.getStatus();

            transactionRepository.save(transaction);

            decisionHistoryRepository.save(
                    new TransactionDecisionHistory(transactionId, analystId, decision, fromStatus, toStatus, reason));

            publishStatusChanged(TransactionStatusChangedEvent.from(transaction, mapDecisionToEventType(decision)));
            triggerPostTransitionSideEffects(transactionId, decision);
            return;
        }

        if (transaction.getStatus() == TransactionStatus.DISPUTED) {
            TransactionStatus fromStatus = transaction.getStatus();
            applyFromDisputed(transaction, decision);
            TransactionStatus toStatus = transaction.getStatus();

            transactionRepository.save(transaction);

            decisionHistoryRepository.save(
                    new TransactionDecisionHistory(transactionId, analystId, decision, fromStatus, toStatus, reason));

            publishStatusChanged(TransactionStatusChangedEvent.from(transaction, mapDecisionToEventType(decision)));
            triggerPostTransitionSideEffectsFromDisputed(transactionId, decision);
            return;
        }

        if (transaction.getStatus() == TransactionStatus.APPROVED && decision == AnalystDecision.APPROVE) {
            finalizeTransactionFinancialUseCase.execute(transactionId);
            return;
        }

        if (transaction.getStatus() == TransactionStatus.DENIED && decision == AnalystDecision.DENY) {
            finalizeTransactionFinancialUseCase.execute(transactionId);
            return;
        }

        if (transaction.getStatus() == TransactionStatus.FRAUD_CONFIRMED && decision == AnalystDecision.CONFIRM_FRAUD) {
            handleFraudConfirmedUseCase.execute(transactionId);
            return;
        }

        if (transaction.getStatus() == TransactionStatus.AWAITING_CUSTOMER
                && decision == AnalystDecision.REQUEST_CUSTOMER_CONFIRMATION) {
            return;
        }

        throw new InvalidAnalystDecisionStateException(transaction.getStatus(), decision);
    }

    private void applyFromAwaitingAnalyst(Transaction transaction, AnalystDecision decision) {
        switch (decision) {
            case APPROVE -> transaction.approve();
            case DENY -> transaction.deny();
            case CONFIRM_FRAUD -> transaction.confirmFraud();
            case REQUEST_CUSTOMER_CONFIRMATION -> transaction.awaitCustomer(LocalDateTime.now().plusMinutes(10));
        }
    }

    private void applyFromDisputed(Transaction transaction, AnalystDecision decision) {
        switch (decision) {
            case APPROVE -> transaction.approve();
            case DENY -> transaction.deny();
            case CONFIRM_FRAUD -> transaction.confirmFraud();
            default -> throw new InvalidAnalystDecisionStateException(transaction.getStatus(), decision);
        }
    }

    private void triggerPostTransitionSideEffects(UUID transactionId, AnalystDecision decision) {
        switch (decision) {
            case APPROVE, DENY -> finalizeTransactionFinancialUseCase.execute(transactionId);
            case CONFIRM_FRAUD -> handleFraudConfirmedUseCase.execute(transactionId);
            case REQUEST_CUSTOMER_CONFIRMATION -> {
                // No financial finalization while awaiting customer confirmation.
            }
        }
    }

    private void triggerPostTransitionSideEffectsFromDisputed(UUID transactionId, AnalystDecision decision) {
        switch (decision) {
            case DENY -> finalizeTransactionFinancialUseCase.execute(transactionId);
            case CONFIRM_FRAUD -> handleFraudConfirmedUseCase.execute(transactionId);
            case APPROVE -> { /* dispute rejected: transaction back to APPROVED, settlement stands */ }
            default -> { /* never reached — applyFromDisputed already threw */ }
        }
    }

    private String mapDecisionToEventType(AnalystDecision decision) {
        return switch (decision) {
            case APPROVE -> EventTypes.TRANSACTION_APPROVED;
            case DENY -> EventTypes.TRANSACTION_DENIED;
            case CONFIRM_FRAUD -> EventTypes.TRANSACTION_FRAUD_CONFIRMED;
            case REQUEST_CUSTOMER_CONFIRMATION -> EventTypes.TRANSACTION_AWAITING_CUSTOMER;
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
