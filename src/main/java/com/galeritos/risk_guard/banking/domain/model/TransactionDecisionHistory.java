package com.galeritos.risk_guard.banking.domain.model;

import java.time.LocalDateTime;
import java.util.UUID;

import com.galeritos.risk_guard.banking.domain.model.enums.AnalystDecision;
import com.galeritos.risk_guard.banking.domain.model.enums.TransactionStatus;

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
@Table(name = "transaction_decision_history")
public class TransactionDecisionHistory {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "transaction_id", nullable = false)
    private UUID transactionId;

    @Column(name = "analyst_id", nullable = false)
    private UUID analystId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private AnalystDecision decision;

    @Enumerated(EnumType.STRING)
    @Column(name = "from_status", nullable = false)
    private TransactionStatus fromStatus;

    @Enumerated(EnumType.STRING)
    @Column(name = "to_status", nullable = false)
    private TransactionStatus toStatus;

    @Column(length = 500)
    private String reason;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    protected TransactionDecisionHistory() {
    }

    public TransactionDecisionHistory(UUID transactionId, UUID analystId, AnalystDecision decision,
            TransactionStatus fromStatus, TransactionStatus toStatus, String reason) {
        this.transactionId = transactionId;
        this.analystId = analystId;
        this.decision = decision;
        this.fromStatus = fromStatus;
        this.toStatus = toStatus;
        this.reason = reason;
        this.createdAt = LocalDateTime.now();
    }
}
