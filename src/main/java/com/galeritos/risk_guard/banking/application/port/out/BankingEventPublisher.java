package com.galeritos.risk_guard.banking.application.port.out;

import com.galeritos.risk_guard.banking.application.event.TransactionCreatedEvent;
import com.galeritos.risk_guard.banking.application.event.TransactionStatusChangedEvent;

public interface BankingEventPublisher {
    void publishTransactionCreated(TransactionCreatedEvent event);

    void publishTransactionStatusChanged(TransactionStatusChangedEvent event);
}
