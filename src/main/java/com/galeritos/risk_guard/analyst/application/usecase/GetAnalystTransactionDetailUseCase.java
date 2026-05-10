package com.galeritos.risk_guard.analyst.application.usecase;

import java.util.List;
import java.util.UUID;

import org.springframework.stereotype.Service;

import com.galeritos.risk_guard.analyst.application.usecase.dto.TransactionDetailAggregate;
import com.galeritos.risk_guard.banking.domain.exception.TransactionNotFoundException;
import com.galeritos.risk_guard.banking.domain.model.Transaction;
import com.galeritos.risk_guard.banking.domain.model.TransactionDecisionHistory;
import com.galeritos.risk_guard.banking.infrastructure.persistence.repository.TransactionDecisionHistoryRepository;
import com.galeritos.risk_guard.banking.infrastructure.persistence.repository.TransactionRepository;
import com.galeritos.risk_guard.identity.domain.exception.UserNotFoundException;
import com.galeritos.risk_guard.identity.domain.model.User;
import com.galeritos.risk_guard.identity.infrastructure.persistence.repository.UserRepository;
import com.galeritos.risk_guard.risk.domain.model.RiskAnalysis;
import com.galeritos.risk_guard.risk.infrastructure.persistence.repository.RiskAnalysisRepository;

@Service
public class GetAnalystTransactionDetailUseCase {

    private final TransactionRepository transactionRepository;
    private final UserRepository userRepository;
    private final RiskAnalysisRepository riskAnalysisRepository;
    private final TransactionDecisionHistoryRepository decisionHistoryRepository;

    public GetAnalystTransactionDetailUseCase(
            TransactionRepository transactionRepository,
            UserRepository userRepository,
            RiskAnalysisRepository riskAnalysisRepository,
            TransactionDecisionHistoryRepository decisionHistoryRepository) {
        this.transactionRepository = transactionRepository;
        this.userRepository = userRepository;
        this.riskAnalysisRepository = riskAnalysisRepository;
        this.decisionHistoryRepository = decisionHistoryRepository;
    }

    public TransactionDetailAggregate execute(UUID transactionId) {
        Transaction transaction = transactionRepository.findById(transactionId)
                .orElseThrow(() -> new TransactionNotFoundException(transactionId));

        User sender = userRepository.findById(transaction.getSenderId())
                .orElseThrow(() -> new UserNotFoundException(transaction.getSenderId()));

        User receiver = userRepository.findById(transaction.getReceiverId())
                .orElseThrow(() -> new UserNotFoundException(transaction.getReceiverId()));

        RiskAnalysis riskAnalysis = riskAnalysisRepository.findByTransactionId(transactionId).orElse(null);

        List<TransactionDecisionHistory> decisionHistory =
                decisionHistoryRepository.findByTransactionIdOrderByCreatedAtDesc(transactionId);

        return new TransactionDetailAggregate(transaction, sender, receiver, riskAnalysis, decisionHistory);
    }
}
