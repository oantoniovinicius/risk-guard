package com.galeritos.risk_guard.risk.application.port.out;

import com.galeritos.risk_guard.risk.application.event.TransactionAnalyzedEvent;

public interface RiskEventPublisher {
    void publishTransactionAnalyzed(TransactionAnalyzedEvent event);
}
