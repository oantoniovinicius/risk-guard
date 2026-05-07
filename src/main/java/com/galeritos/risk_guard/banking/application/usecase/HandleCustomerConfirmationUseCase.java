package com.galeritos.risk_guard.banking.application.usecase;

import java.util.UUID;

import org.springframework.stereotype.Service;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import com.galeritos.risk_guard.banking.application.event.TransactionStatusChangedEvent;
import com.galeritos.risk_guard.banking.application.port.out.BankingEventPublisher;
import com.galeritos.risk_guard.banking.domain.exception.InvalidCustomerConfirmationStateException;
import com.galeritos.risk_guard.banking.domain.exception.TransactionNotFoundException;
import com.galeritos.risk_guard.banking.domain.model.Transaction;
import com.galeritos.risk_guard.banking.domain.model.enums.CustomerConfirmationDecision;
import com.galeritos.risk_guard.banking.domain.model.enums.TransactionStatus;
import com.galeritos.risk_guard.banking.infrastructure.persistence.repository.TransactionRepository;
import com.galeritos.risk_guard.identity.application.security.AuthenticatedUser;
import com.galeritos.risk_guard.identity.application.security.CurrentUserProvider;
import com.galeritos.risk_guard.identity.application.service.PinService;
import com.galeritos.risk_guard.shared.events.EventTypes;

import jakarta.transaction.Transactional;

@Service
public class HandleCustomerConfirmationUseCase {
    private final TransactionRepository transactionRepository;
    private final FinalizeTransactionFinancialUseCase finalizeTransactionFinancialUseCase;
    private final HandleFraudConfirmedUseCase handleFraudConfirmedUseCase;
    private final BankingEventPublisher eventPublisher;
    private final PinService pinService;
    private final CurrentUserProvider currentUserProvider;

    public HandleCustomerConfirmationUseCase(
            TransactionRepository transactionRepository,
            FinalizeTransactionFinancialUseCase finalizeTransactionFinancialUseCase,
            HandleFraudConfirmedUseCase handleFraudConfirmedUseCase,
            BankingEventPublisher eventPublisher,
            PinService pinService,
            CurrentUserProvider currentUserProvider) {
        this.transactionRepository = transactionRepository;
        this.finalizeTransactionFinancialUseCase = finalizeTransactionFinancialUseCase;
        this.handleFraudConfirmedUseCase = handleFraudConfirmedUseCase;
        this.eventPublisher = eventPublisher;
        this.pinService = pinService;
        this.currentUserProvider = currentUserProvider;
    }

    @Transactional
    public void execute(UUID transactionId, CustomerConfirmationDecision decision, String pin) {
        Transaction transaction = transactionRepository.findByIdForUpdate(transactionId)
                .orElseThrow(() -> new TransactionNotFoundException(transactionId));

        AuthenticatedUser user = currentUserProvider.getAuthenticatedUser();
        pinService.validate(user.userId(), pin);

        if (transaction.getStatus() == TransactionStatus.AWAITING_CUSTOMER) {
            applyFromAwaitingCustomer(transaction, decision);
            transactionRepository.save(transaction);
            publishStatusChanged(TransactionStatusChangedEvent.from(transaction, mapDecisionToEventType(decision)));
            triggerPostTransitionSideEffects(transactionId, decision);
            return;
        }

        if (transaction.getStatus() == TransactionStatus.APPROVED
                && decision == CustomerConfirmationDecision.APPROVE) {
            finalizeTransactionFinancialUseCase.execute(transactionId);
            return;
        }

        if (transaction.getStatus() == TransactionStatus.FRAUD_CONFIRMED
                && decision == CustomerConfirmationDecision.REPORT_FRAUD) {
            handleFraudConfirmedUseCase.execute(transactionId);
            return;
        }

        throw new InvalidCustomerConfirmationStateException(transaction.getStatus(), decision);
    }

    private void applyFromAwaitingCustomer(Transaction transaction, CustomerConfirmationDecision decision) {
        switch (decision) {
            case APPROVE -> transaction.approve();
            case REPORT_FRAUD -> transaction.confirmFraud();
        }
    }

    private void triggerPostTransitionSideEffects(UUID transactionId, CustomerConfirmationDecision decision) {
        switch (decision) {
            case APPROVE -> finalizeTransactionFinancialUseCase.execute(transactionId);
            case REPORT_FRAUD -> handleFraudConfirmedUseCase.execute(transactionId);
        }
    }

    private String mapDecisionToEventType(CustomerConfirmationDecision decision) {
        return switch (decision) {
            case APPROVE -> EventTypes.TRANSACTION_APPROVED;
            case REPORT_FRAUD -> EventTypes.TRANSACTION_FRAUD_CONFIRMED;
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
