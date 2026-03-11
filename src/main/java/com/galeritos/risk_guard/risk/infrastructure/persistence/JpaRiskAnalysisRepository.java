package com.galeritos.risk_guard.risk.infrastructure.persistence;

import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.galeritos.risk_guard.risk.domain.model.RiskAnalysis;
import com.galeritos.risk_guard.risk.domain.repository.RiskAnalysisRepository;

@Repository
public interface JpaRiskAnalysisRepository
        extends JpaRepository<RiskAnalysis, UUID>, RiskAnalysisRepository {

    Optional<RiskAnalysis> findByTransactionId(UUID transactionId);

    boolean existsByTransactionId(UUID transactionId);

}