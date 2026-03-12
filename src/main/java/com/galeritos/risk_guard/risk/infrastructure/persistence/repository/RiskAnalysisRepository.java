package com.galeritos.risk_guard.risk.infrastructure.persistence.repository;

import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

import com.galeritos.risk_guard.risk.domain.model.RiskAnalysis;

public interface RiskAnalysisRepository extends JpaRepository<RiskAnalysis, UUID> {

    Optional<RiskAnalysis> findByTransactionId(UUID transactionId);

    boolean existsByTransactionId(UUID transactionId);

}
