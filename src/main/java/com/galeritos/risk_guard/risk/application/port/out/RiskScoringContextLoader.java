package com.galeritos.risk_guard.risk.application.port.out;

import com.galeritos.risk_guard.banking.application.event.TransactionCreatedEvent;
import com.galeritos.risk_guard.risk.domain.scoring.RiskScoringContext;

public interface RiskScoringContextLoader {
    RiskScoringContext load(TransactionCreatedEvent event);
}
