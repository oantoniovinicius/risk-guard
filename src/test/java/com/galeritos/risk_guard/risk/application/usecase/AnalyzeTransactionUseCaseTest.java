package com.galeritos.risk_guard.risk.application.usecase;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
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
import com.galeritos.risk_guard.risk.domain.model.RiskAnalysis;
import com.galeritos.risk_guard.risk.infrastructure.persistence.repository.RiskAnalysisRepository;
import com.galeritos.risk_guard.shared.enums.RiskLevel;
import com.galeritos.risk_guard.shared.events.EventTypes;

@ExtendWith(MockitoExtension.class)
class AnalyzeTransactionUseCaseTest {

    @Mock
    private RiskAnalysisRepository riskAnalysisRepository;

    @Mock
    private RiskEventPublisher riskEventPublisher;

    @InjectMocks
    private AnalyzeTransactionUseCase useCase;

    @Test
    void shouldPersistRiskAnalysisAndPublishTransactionAnalyzedEvent() {
        UUID transactionId = UUID.randomUUID();
        TransactionCreatedEvent event = new TransactionCreatedEvent(
                UUID.randomUUID(),
                EventTypes.TRANSACTION_CREATED,
                transactionId,
                new BigDecimal("1200.00"),
                UUID.randomUUID(),
                UUID.randomUUID());

        when(riskAnalysisRepository.existsByTransactionId(transactionId)).thenReturn(false);
        when(riskAnalysisRepository.save(any(RiskAnalysis.class))).thenAnswer(invocation -> {
            RiskAnalysis riskAnalysis = invocation.getArgument(0);
            ReflectionTestUtils.setField(riskAnalysis, "id", UUID.randomUUID());
            return riskAnalysis;
        });

        useCase.execute(event);

        ArgumentCaptor<RiskAnalysis> riskCaptor = ArgumentCaptor.forClass(RiskAnalysis.class);
        verify(riskAnalysisRepository).save(riskCaptor.capture());
        RiskAnalysis savedAnalysis = riskCaptor.getValue();
        assertEquals(transactionId, savedAnalysis.getTransactionId());
        assertEquals(0, new BigDecimal("0.92").compareTo(savedAnalysis.getScore()));
        assertEquals(RiskLevel.HIGH, savedAnalysis.getRiskLevel());
        assertEquals("Mock high-risk rule based on amount", savedAnalysis.getExplanation());
        assertEquals("mock-risk-v1", savedAnalysis.getModelVersion());

        ArgumentCaptor<TransactionAnalyzedEvent> eventCaptor = ArgumentCaptor.forClass(TransactionAnalyzedEvent.class);
        verify(riskEventPublisher).publishTransactionAnalyzed(eventCaptor.capture());
        TransactionAnalyzedEvent publishedEvent = eventCaptor.getValue();
        assertNotNull(publishedEvent.eventId());
        assertEquals(EventTypes.TRANSACTION_ANALYZED, publishedEvent.eventType());
        assertEquals(transactionId, publishedEvent.transactionId());
        assertEquals(RiskLevel.HIGH, publishedEvent.riskLevel());
        assertEquals(0, new BigDecimal("0.92").compareTo(publishedEvent.score()));
        assertEquals("Mock high-risk rule based on amount", publishedEvent.explanation());
    }

    @Test
    void shouldClassifyMediumRiskAndPublishEvent() {
        UUID transactionId = UUID.randomUUID();
        TransactionCreatedEvent event = new TransactionCreatedEvent(
                UUID.randomUUID(),
                EventTypes.TRANSACTION_CREATED,
                transactionId,
                new BigDecimal("500.00"),
                UUID.randomUUID(),
                UUID.randomUUID());

        when(riskAnalysisRepository.existsByTransactionId(transactionId)).thenReturn(false);
        when(riskAnalysisRepository.save(any(RiskAnalysis.class))).thenAnswer(invocation -> invocation.getArgument(0));

        useCase.execute(event);

        ArgumentCaptor<RiskAnalysis> riskCaptor = ArgumentCaptor.forClass(RiskAnalysis.class);
        verify(riskAnalysisRepository).save(riskCaptor.capture());
        RiskAnalysis savedAnalysis = riskCaptor.getValue();
        assertEquals(0, new BigDecimal("0.61").compareTo(savedAnalysis.getScore()));
        assertEquals(RiskLevel.MEDIUM, savedAnalysis.getRiskLevel());
        assertEquals("Mock medium-risk rule based on amount", savedAnalysis.getExplanation());

        ArgumentCaptor<TransactionAnalyzedEvent> eventCaptor = ArgumentCaptor.forClass(TransactionAnalyzedEvent.class);
        verify(riskEventPublisher).publishTransactionAnalyzed(eventCaptor.capture());
        TransactionAnalyzedEvent publishedEvent = eventCaptor.getValue();
        assertEquals(RiskLevel.MEDIUM, publishedEvent.riskLevel());
        assertEquals(0, new BigDecimal("0.61").compareTo(publishedEvent.score()));
        assertEquals("Mock medium-risk rule based on amount", publishedEvent.explanation());
    }

    @Test
    void shouldClassifyLowRiskAndPublishEvent() {
        UUID transactionId = UUID.randomUUID();
        TransactionCreatedEvent event = new TransactionCreatedEvent(
                UUID.randomUUID(),
                EventTypes.TRANSACTION_CREATED,
                transactionId,
                new BigDecimal("100.00"),
                UUID.randomUUID(),
                UUID.randomUUID());

        when(riskAnalysisRepository.existsByTransactionId(transactionId)).thenReturn(false);
        when(riskAnalysisRepository.save(any(RiskAnalysis.class))).thenAnswer(invocation -> invocation.getArgument(0));

        useCase.execute(event);

        ArgumentCaptor<RiskAnalysis> riskCaptor = ArgumentCaptor.forClass(RiskAnalysis.class);
        verify(riskAnalysisRepository).save(riskCaptor.capture());
        RiskAnalysis savedAnalysis = riskCaptor.getValue();
        assertEquals(0, new BigDecimal("0.18").compareTo(savedAnalysis.getScore()));
        assertEquals(RiskLevel.LOW, savedAnalysis.getRiskLevel());
        assertEquals("Mock low-risk rule based on amount", savedAnalysis.getExplanation());

        ArgumentCaptor<TransactionAnalyzedEvent> eventCaptor = ArgumentCaptor.forClass(TransactionAnalyzedEvent.class);
        verify(riskEventPublisher).publishTransactionAnalyzed(eventCaptor.capture());
        TransactionAnalyzedEvent publishedEvent = eventCaptor.getValue();
        assertEquals(RiskLevel.LOW, publishedEvent.riskLevel());
        assertEquals(0, new BigDecimal("0.18").compareTo(publishedEvent.score()));
        assertEquals("Mock low-risk rule based on amount", publishedEvent.explanation());
    }

    @Test
    void shouldIgnoreAlreadyAnalyzedTransaction() {
        UUID transactionId = UUID.randomUUID();
        TransactionCreatedEvent event = new TransactionCreatedEvent(
                UUID.randomUUID(),
                EventTypes.TRANSACTION_CREATED,
                transactionId,
                new BigDecimal("80.00"),
                UUID.randomUUID(),
                UUID.randomUUID());

        when(riskAnalysisRepository.existsByTransactionId(transactionId)).thenReturn(true);

        useCase.execute(event);

        verify(riskAnalysisRepository, never()).save(any());
        verify(riskEventPublisher, never()).publishTransactionAnalyzed(any());
    }
}
