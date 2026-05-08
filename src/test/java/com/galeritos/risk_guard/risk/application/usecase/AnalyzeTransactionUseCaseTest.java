package com.galeritos.risk_guard.risk.application.usecase;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

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
import com.galeritos.risk_guard.shared.events.EventTypes;

@ExtendWith(MockitoExtension.class)
class AnalyzeTransactionUseCaseTest {

    @Mock
    private RiskAnalysisRepository riskAnalysisRepository;

    @Mock
    private RiskEventPublisher riskEventPublisher;

    @Mock
    private RiskScoringStrategy scoringStrategy;

    @Mock
    private RiskScoringContextLoader contextLoader;

    @Mock
    private RiskThresholdsProvider thresholdsProvider;

    @InjectMocks
    private AnalyzeTransactionUseCase useCase;

    private TransactionCreatedEvent buildEvent(BigDecimal amount) {
        return new TransactionCreatedEvent(
                UUID.randomUUID(),
                EventTypes.TRANSACTION_CREATED,
                UUID.randomUUID(),
                amount,
                UUID.randomUUID(),
                UUID.randomUUID());
    }

    private RiskScoringContext stubContext() {
        return new RiskScoringContext(
                UUID.randomUUID(),
                BigDecimal.TEN,
                LocalDateTime.now(),
                1L, 3L, false, BigDecimal.TEN, false, 0L, false, 0L);
    }

    @Test
    void shouldPersistAndPublishHighRiskAnalysis() {
        TransactionCreatedEvent event = buildEvent(new BigDecimal("1200.00"));
        RiskScoringContext ctx = stubContext();
        BigDecimal score = new BigDecimal("0.92");
        String explanation = "amount R$1200.00 is 4.0× above 30-day avg (R$300.00)";

        when(riskAnalysisRepository.existsByTransactionId(event.aggregateId())).thenReturn(false);
        when(contextLoader.load(event)).thenReturn(ctx);
        when(scoringStrategy.score(ctx)).thenReturn(new RiskScore(score, explanation));
        when(thresholdsProvider.highRiskThreshold()).thenReturn(new BigDecimal("0.90"));
        when(riskAnalysisRepository.save(any(RiskAnalysis.class))).thenAnswer(inv -> {
            RiskAnalysis ra = inv.getArgument(0);
            ReflectionTestUtils.setField(ra, "id", UUID.randomUUID());
            return ra;
        });

        useCase.execute(event);

        ArgumentCaptor<RiskAnalysis> riskCaptor = ArgumentCaptor.forClass(RiskAnalysis.class);
        verify(riskAnalysisRepository).save(riskCaptor.capture());
        RiskAnalysis saved = riskCaptor.getValue();
        assertEquals(event.aggregateId(), saved.getTransactionId());
        assertEquals(0, score.compareTo(saved.getScore()));
        assertEquals(RiskLevel.HIGH, saved.getRiskLevel());
        assertEquals(explanation, saved.getExplanation());
        assertEquals("rule-engine-v1", saved.getModelVersion());

        ArgumentCaptor<TransactionAnalyzedEvent> eventCaptor = ArgumentCaptor.forClass(TransactionAnalyzedEvent.class);
        verify(riskEventPublisher).publishTransactionAnalyzed(eventCaptor.capture());
        TransactionAnalyzedEvent published = eventCaptor.getValue();
        assertNotNull(published.eventId());
        assertEquals(EventTypes.TRANSACTION_ANALYZED, published.eventType());
        assertEquals(RiskLevel.HIGH, published.riskLevel());
        assertEquals(explanation, published.explanation());
    }

    @Test
    void shouldClassifyMediumRiskByThreshold() {
        TransactionCreatedEvent event = buildEvent(new BigDecimal("500.00"));
        RiskScoringContext ctx = stubContext();
        BigDecimal score = new BigDecimal("0.65");

        when(riskAnalysisRepository.existsByTransactionId(event.aggregateId())).thenReturn(false);
        when(contextLoader.load(event)).thenReturn(ctx);
        when(scoringStrategy.score(ctx)).thenReturn(new RiskScore(score, "some medium signal"));
        when(thresholdsProvider.highRiskThreshold()).thenReturn(new BigDecimal("0.90"));
        when(thresholdsProvider.mediumRiskThreshold()).thenReturn(new BigDecimal("0.60"));
        when(riskAnalysisRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        useCase.execute(event);

        ArgumentCaptor<RiskAnalysis> captor = ArgumentCaptor.forClass(RiskAnalysis.class);
        verify(riskAnalysisRepository).save(captor.capture());
        assertEquals(RiskLevel.MEDIUM, captor.getValue().getRiskLevel());
    }

    @Test
    void shouldClassifyLowRiskByThreshold() {
        TransactionCreatedEvent event = buildEvent(new BigDecimal("50.00"));
        RiskScoringContext ctx = stubContext();
        BigDecimal score = new BigDecimal("0.10");

        when(riskAnalysisRepository.existsByTransactionId(event.aggregateId())).thenReturn(false);
        when(contextLoader.load(event)).thenReturn(ctx);
        when(scoringStrategy.score(ctx)).thenReturn(new RiskScore(score, "no significant risk signals detected"));
        when(thresholdsProvider.highRiskThreshold()).thenReturn(new BigDecimal("0.90"));
        when(thresholdsProvider.mediumRiskThreshold()).thenReturn(new BigDecimal("0.60"));
        when(riskAnalysisRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        useCase.execute(event);

        ArgumentCaptor<RiskAnalysis> captor = ArgumentCaptor.forClass(RiskAnalysis.class);
        verify(riskAnalysisRepository).save(captor.capture());
        assertEquals(RiskLevel.LOW, captor.getValue().getRiskLevel());
    }

    @Test
    void shouldIgnoreAlreadyAnalyzedTransaction() {
        TransactionCreatedEvent event = buildEvent(new BigDecimal("80.00"));
        when(riskAnalysisRepository.existsByTransactionId(event.aggregateId())).thenReturn(true);

        useCase.execute(event);

        verify(riskAnalysisRepository, never()).save(any());
        verify(riskEventPublisher, never()).publishTransactionAnalyzed(any());
    }
}
