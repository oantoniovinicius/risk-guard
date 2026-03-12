package com.galeritos.risk_guard.banking.application.port.out;

import com.galeritos.risk_guard.banking.application.event.TransactionCreatedEvent;

public interface EventPublisher {
    void publishTransactionCreated(TransactionCreatedEvent event);
}
