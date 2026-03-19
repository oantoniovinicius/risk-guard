package com.galeritos.risk_guard.banking.infrastructure.messaging;

import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;

import com.galeritos.risk_guard.banking.application.usecase.ApplyTransactionAnalysisUseCase;
import com.galeritos.risk_guard.risk.application.event.TransactionAnalyzedEvent;

@Component
public class TransactionAnalyzedListener {
    private final ApplyTransactionAnalysisUseCase applyTransactionAnalysisUseCase;

    public TransactionAnalyzedListener(ApplyTransactionAnalysisUseCase applyTransactionAnalysisUseCase) {
        this.applyTransactionAnalysisUseCase = applyTransactionAnalysisUseCase;
    }

    @RabbitListener(queues = "#{transactionAnalyzedQueue.name}")
    public void onTransactionAnalyzed(TransactionAnalyzedEvent event) {
        applyTransactionAnalysisUseCase.execute(event);
    }
}
