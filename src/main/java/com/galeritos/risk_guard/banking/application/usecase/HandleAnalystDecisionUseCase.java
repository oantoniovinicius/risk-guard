package com.galeritos.risk_guard.banking.application.usecase;

import java.time.LocalDateTime;
import java.util.UUID;

import org.springframework.stereotype.Service;

import com.galeritos.risk_guard.banking.domain.exception.InvalidAnalystDecisionStateException;
import com.galeritos.risk_guard.banking.domain.exception.TransactionNotFoundException;
import com.galeritos.risk_guard.banking.domain.model.Transaction;
import com.galeritos.risk_guard.banking.domain.model.enums.AnalystDecision;
import com.galeritos.risk_guard.banking.domain.model.enums.TransactionStatus;
import com.galeritos.risk_guard.banking.infrastructure.persistence.repository.TransactionRepository;

import jakarta.transaction.Transactional;

@Service
public class HandleAnalystDecisionUseCase {
    private final TransactionRepository transactionRepository;
    private final FinalizeTransactionFinancialUseCase finalizeTransactionFinancialUseCase;
    private final HandleFraudConfirmedUseCase handleFraudConfirmedUseCase;

    public HandleAnalystDecisionUseCase(
            TransactionRepository transactionRepository,
            FinalizeTransactionFinancialUseCase finalizeTransactionFinancialUseCase,
            HandleFraudConfirmedUseCase handleFraudConfirmedUseCase) {
        this.transactionRepository = transactionRepository;
        this.finalizeTransactionFinancialUseCase = finalizeTransactionFinancialUseCase;
        this.handleFraudConfirmedUseCase = handleFraudConfirmedUseCase;
    }

    @Transactional
    public void execute(UUID transactionId, AnalystDecision decision) {
        Transaction transaction = transactionRepository.findByIdForUpdate(transactionId)
                .orElseThrow(() -> new TransactionNotFoundException(transactionId));

        if (transaction.getStatus() == TransactionStatus.AWAITING_ANALYST) {
            applyFromAwaitingAnalyst(transaction, decision);
            transactionRepository.save(transaction);
            triggerPostTransitionSideEffects(transactionId, decision);
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

    private void triggerPostTransitionSideEffects(UUID transactionId, AnalystDecision decision) {
        switch (decision) {
            case APPROVE, DENY -> finalizeTransactionFinancialUseCase.execute(transactionId);
            case CONFIRM_FRAUD -> handleFraudConfirmedUseCase.execute(transactionId);
            case REQUEST_CUSTOMER_CONFIRMATION -> {
                // No financial finalization while awaiting customer confirmation.
            }
        }
    }
}
