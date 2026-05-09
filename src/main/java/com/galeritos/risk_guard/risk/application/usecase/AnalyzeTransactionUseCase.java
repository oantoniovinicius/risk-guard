package com.galeritos.risk_guard.risk.application.usecase;

import java.math.BigDecimal;

import org.springframework.stereotype.Service;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import com.galeritos.risk_guard.banking.application.event.TransactionCreatedEvent;
import com.galeritos.risk_guard.risk.application.event.TransactionAnalyzedEvent;
import com.galeritos.risk_guard.risk.application.port.out.RiskEventPublisher;
import com.galeritos.risk_guard.risk.application.port.out.RiskScoringContextLoader;
import com.galeritos.risk_guard.risk.application.port.out.RiskThresholdsProvider;
import com.galeritos.risk_guard.risk.domain.model.RiskAnalysis;
import com.galeritos.risk_guard.risk.domain.scoring.RiskScore;
import com.galeritos.risk_guard.risk.domain.scoring.RiskScoringContext;
import com.galeritos.risk_guard.risk.domain.scoring.RiskScoringStrategy;
import com.galeritos.risk_guard.risk.infrastructure.persistence.repository.RiskAnalysisRepository;
import com.galeritos.risk_guard.shared.enums.RiskLevel;

import jakarta.transaction.Transactional;

@Service
public class AnalyzeTransactionUseCase {

    private static final String MODEL_VERSION = "rule-engine-v1";

    private final RiskAnalysisRepository riskAnalysisRepository;
    private final RiskEventPublisher riskEventPublisher;
    private final RiskScoringStrategy scoringStrategy;
    private final RiskScoringContextLoader contextLoader;
    private final RiskThresholdsProvider thresholdsProvider;

    public AnalyzeTransactionUseCase(
            RiskAnalysisRepository riskAnalysisRepository,
            RiskEventPublisher riskEventPublisher,
            RiskScoringStrategy scoringStrategy,
            RiskScoringContextLoader contextLoader,
            RiskThresholdsProvider thresholdsProvider) {
        this.riskAnalysisRepository = riskAnalysisRepository;
        this.riskEventPublisher = riskEventPublisher;
        this.scoringStrategy = scoringStrategy;
        this.contextLoader = contextLoader;
        this.thresholdsProvider = thresholdsProvider;
    }

    @Transactional
    public void execute(TransactionCreatedEvent event) {
        if (riskAnalysisRepository.existsByTransactionId(event.aggregateId())) {
            return;
        }

        RiskScoringContext context = contextLoader.load(event);
        RiskScore riskScore = scoringStrategy.score(context);
        RiskLevel riskLevel = classifyRiskLevel(riskScore.score());

        RiskAnalysis riskAnalysis = new RiskAnalysis(
                null,
                event.aggregateId(),
                riskScore.score(),
                riskLevel,
                riskScore.explanation(),
                MODEL_VERSION);

        riskAnalysis = riskAnalysisRepository.save(riskAnalysis);
        publishAfterCommit(TransactionAnalyzedEvent.from(riskAnalysis));
    }

    private RiskLevel classifyRiskLevel(BigDecimal score) {
        if (score.compareTo(thresholdsProvider.highRiskThreshold()) >= 0) return RiskLevel.HIGH;
        if (score.compareTo(thresholdsProvider.mediumRiskThreshold()) >= 0) return RiskLevel.MEDIUM;
        return RiskLevel.LOW;
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
}
