package com.galeritos.risk_guard.risk.domain.model;

import java.math.BigDecimal;
import java.util.UUID;

import com.galeritos.risk_guard.shared.enums.RiskLevel;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Getter;

@Entity
@Getter
@Table(name = "risk_analysis")
public class RiskAnalysis {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "transaction_id", nullable = false, unique = true)
    private UUID transactionId;

    @Column(nullable = false)
    private BigDecimal score;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private RiskLevel riskLevel;

    private String explanation; // reason

    private String modelVersion;

    protected RiskAnalysis() {
    }

    public RiskAnalysis(UUID id, UUID transactionId, BigDecimal score, RiskLevel riskLevel, String explanation,
            String modelVersion) {
        this.id = id;
        this.transactionId = transactionId;
        this.score = score;
        this.riskLevel = riskLevel;
        this.explanation = explanation;
        this.modelVersion = modelVersion;
    }
}
