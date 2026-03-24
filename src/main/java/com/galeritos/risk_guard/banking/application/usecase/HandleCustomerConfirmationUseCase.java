package com.galeritos.risk_guard.banking.application.usecase;

import java.util.UUID;

import org.springframework.stereotype.Service;

import com.galeritos.risk_guard.banking.domain.exception.InvalidCustomerConfirmationStateException;
import com.galeritos.risk_guard.banking.domain.exception.TransactionNotFoundException;
import com.galeritos.risk_guard.banking.domain.model.Transaction;
import com.galeritos.risk_guard.banking.domain.model.enums.CustomerConfirmationDecision;
import com.galeritos.risk_guard.banking.domain.model.enums.TransactionStatus;
import com.galeritos.risk_guard.banking.infrastructure.persistence.repository.TransactionRepository;

import jakarta.transaction.Transactional;

@Service
public class HandleCustomerConfirmationUseCase {
    private final TransactionRepository transactionRepository;
    private final FinalizeTransactionFinancialUseCase finalizeTransactionFinancialUseCase;
    private final HandleFraudConfirmedUseCase handleFraudConfirmedUseCase;

    public HandleCustomerConfirmationUseCase(
            TransactionRepository transactionRepository,
            FinalizeTransactionFinancialUseCase finalizeTransactionFinancialUseCase,
            HandleFraudConfirmedUseCase handleFraudConfirmedUseCase) {
        this.transactionRepository = transactionRepository;
        this.finalizeTransactionFinancialUseCase = finalizeTransactionFinancialUseCase;
        this.handleFraudConfirmedUseCase = handleFraudConfirmedUseCase;
    }

    @Transactional
    public void execute(UUID transactionId, CustomerConfirmationDecision decision) {
        Transaction transaction = transactionRepository.findByIdForUpdate(transactionId)
                .orElseThrow(() -> new TransactionNotFoundException(transactionId));

        if (transaction.getStatus() == TransactionStatus.AWAITING_CUSTOMER) {
            applyFromAwaitingCustomer(transaction, decision);
            transactionRepository.save(transaction);
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
}
