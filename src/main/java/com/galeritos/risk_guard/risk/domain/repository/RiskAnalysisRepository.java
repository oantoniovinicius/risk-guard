package com.galeritos.risk_guard.risk.domain.repository;

import java.util.Optional;
import java.util.UUID;

import com.galeritos.risk_guard.risk.domain.model.RiskAnalysis;

public interface RiskAnalysisRepository {

    Optional<RiskAnalysis> findByTransactionId(UUID transactionId);

    boolean existsByTransactionId(UUID transactionId);

    RiskAnalysis save(RiskAnalysis analysis);

}