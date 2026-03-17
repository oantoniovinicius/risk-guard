package com.galeritos.risk_guard.risk.infrastructure.messaging;

import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;

import com.galeritos.risk_guard.banking.application.event.TransactionCreatedEvent;
import com.galeritos.risk_guard.risk.application.usecase.AnalyzeTransactionUseCase;

@Component
public class TransactionCreatedListener {
    private final AnalyzeTransactionUseCase analyzeTransactionUseCase;

    public TransactionCreatedListener(AnalyzeTransactionUseCase analyzeTransactionUseCase) {
        this.analyzeTransactionUseCase = analyzeTransactionUseCase;
    }

    @RabbitListener(queues = "#{transactionCreatedQueue.name}")
    public void onTransactionCreated(TransactionCreatedEvent event) {
        analyzeTransactionUseCase.execute(event);
    }
}
