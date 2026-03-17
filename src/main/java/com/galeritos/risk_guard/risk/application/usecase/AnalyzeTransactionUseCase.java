package com.galeritos.risk_guard.risk.application.usecase;

import java.math.BigDecimal;

import org.springframework.stereotype.Service;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import com.galeritos.risk_guard.banking.application.event.TransactionCreatedEvent;
import com.galeritos.risk_guard.risk.application.event.TransactionAnalyzedEvent;
import com.galeritos.risk_guard.risk.application.port.out.RiskEventPublisher;
import com.galeritos.risk_guard.risk.domain.model.RiskAnalysis;
import com.galeritos.risk_guard.risk.infrastructure.persistence.repository.RiskAnalysisRepository;
import com.galeritos.risk_guard.shared.enums.RiskLevel;

import jakarta.transaction.Transactional;

@Service
public class AnalyzeTransactionUseCase {
    private static final String MODEL_VERSION = "mock-risk-v1";

    private final RiskAnalysisRepository riskAnalysisRepository;
    private final RiskEventPublisher riskEventPublisher;

    public AnalyzeTransactionUseCase(
            RiskAnalysisRepository riskAnalysisRepository,
            RiskEventPublisher riskEventPublisher) {
        this.riskAnalysisRepository = riskAnalysisRepository;
        this.riskEventPublisher = riskEventPublisher;
    }

    @Transactional
    public void execute(TransactionCreatedEvent event) {
        if (riskAnalysisRepository.existsByTransactionId(event.aggregateId())) {
            return;
        }

        RiskAnalysis riskAnalysis = buildRiskAnalysis(event);
        riskAnalysis = riskAnalysisRepository.save(riskAnalysis);
        publishAfterCommit(TransactionAnalyzedEvent.from(riskAnalysis));
    }

    private RiskAnalysis buildRiskAnalysis(TransactionCreatedEvent event) {
        RiskProfile profile = evaluate(event.amount());

        return new RiskAnalysis(
                null,
                event.aggregateId(),
                profile.score(),
                profile.riskLevel(),
                profile.explanation(),
                MODEL_VERSION);
    }

    private RiskProfile evaluate(BigDecimal amount) {
        if (amount.compareTo(new BigDecimal("1000.00")) >= 0) {
            return new RiskProfile(
                    new BigDecimal("0.92"),
                    RiskLevel.HIGH,
                    "Mock high-risk rule based on amount");
        }

        if (amount.compareTo(new BigDecimal("300.00")) >= 0) {
            return new RiskProfile(
                    new BigDecimal("0.61"),
                    RiskLevel.MEDIUM,
                    "Mock medium-risk rule based on amount");
        }

        return new RiskProfile(
                new BigDecimal("0.18"),
                RiskLevel.LOW,
                "Mock low-risk rule based on amount");
    }

    private void publishAfterCommit(TransactionAnalyzedEvent event) {
        if (!TransactionSynchronizationManager.isSynchronizationActive()) {
            riskEventPublisher.publishTransactionAnalyzed(event);
            return;
        }

        TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
            @Override
            public void afterCommit() {
                riskEventPublisher.publishTransactionAnalyzed(event);
            }
        });
    }

    private record RiskProfile(BigDecimal score, RiskLevel riskLevel, String explanation) {
    }
}
