package com.galeritos.risk_guard.banking.domain.model;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.EnumMap;
import java.util.EnumSet;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

import com.galeritos.risk_guard.banking.domain.exception.InvalidTransactionStateTransitionException;
import com.galeritos.risk_guard.banking.domain.model.enums.FinancialStatus;
import com.galeritos.risk_guard.banking.domain.model.enums.TransactionStatus;
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
@Table(name = "transactions")
public class Transaction {
    private static final Map<TransactionStatus, Set<TransactionStatus>> ALLOWED_TRANSITIONS = new EnumMap<>(
            TransactionStatus.class);

    static {
        ALLOWED_TRANSITIONS.put(TransactionStatus.CREATED, EnumSet.of(TransactionStatus.ANALYZING));
        ALLOWED_TRANSITIONS.put(TransactionStatus.ANALYZING, EnumSet.of(
                TransactionStatus.APPROVED,
                TransactionStatus.AWAITING_CUSTOMER,
                TransactionStatus.AWAITING_ANALYST,
                TransactionStatus.DENIED,
                TransactionStatus.FRAUD_CONFIRMED));
        ALLOWED_TRANSITIONS.put(TransactionStatus.AWAITING_CUSTOMER, EnumSet.of(
                TransactionStatus.APPROVED,
                TransactionStatus.FRAUD_CONFIRMED,
                TransactionStatus.AWAITING_ANALYST));
        ALLOWED_TRANSITIONS.put(TransactionStatus.AWAITING_ANALYST, EnumSet.of(
                TransactionStatus.APPROVED,
                TransactionStatus.DENIED,
                TransactionStatus.FRAUD_CONFIRMED,
                TransactionStatus.AWAITING_CUSTOMER));
        ALLOWED_TRANSITIONS.put(TransactionStatus.APPROVED, EnumSet.of(TransactionStatus.DISPUTED));
        ALLOWED_TRANSITIONS.put(TransactionStatus.DISPUTED, EnumSet.of(
                TransactionStatus.APPROVED,
                TransactionStatus.DENIED,
                TransactionStatus.FRAUD_CONFIRMED));
        ALLOWED_TRANSITIONS.put(TransactionStatus.DENIED, EnumSet.noneOf(TransactionStatus.class));
        ALLOWED_TRANSITIONS.put(TransactionStatus.FRAUD_CONFIRMED, EnumSet.noneOf(TransactionStatus.class));
    }

    @Id
    @Column(nullable = false, unique = true)
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "sender_id", nullable = false)
    private UUID senderId;

    @Column(name = "receiver_id", nullable = false)
    private UUID receiverId;

    @Column(nullable = false)
    private BigDecimal amount;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private TransactionStatus status;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private FinancialStatus financialStatus;

    @Enumerated(EnumType.STRING)
    private RiskLevel riskLevel;

    @Column(nullable = false)
    private LocalDateTime createdAt;

    protected Transaction() {
    }

    public Transaction(UUID senderId, UUID receiverId, BigDecimal amount, TransactionStatus status,
            FinancialStatus financialStatus, RiskLevel riskLevel, LocalDateTime createdAt) {
        this.senderId = senderId;
        this.receiverId = receiverId;
        this.amount = amount;
        this.status = status;
        this.financialStatus = financialStatus;
        this.riskLevel = riskLevel;
        this.createdAt = createdAt;
    }

    public boolean canTransitionTo(TransactionStatus newStatus) {
        Set<TransactionStatus> allowed = ALLOWED_TRANSITIONS.getOrDefault(this.status, Set.of());
        return allowed.contains(newStatus);
    }

    public void startAnalyzing() {
        transitionTo(TransactionStatus.ANALYZING);
    }

    public void awaitCustomer() {
        transitionTo(TransactionStatus.AWAITING_CUSTOMER);
    }

    public void awaitAnalyst() {
        transitionTo(TransactionStatus.AWAITING_ANALYST);
    }

    public void approve() {
        transitionTo(TransactionStatus.APPROVED);
    }

    public void deny() {
        transitionTo(TransactionStatus.DENIED);
    }

    public void confirmFraud() {
        transitionTo(TransactionStatus.FRAUD_CONFIRMED);
    }

    public void dispute() {
        transitionTo(TransactionStatus.DISPUTED);
    }

    public void settle() {
        if (this.status != TransactionStatus.APPROVED) {
            throw new IllegalStateException("Only APPROVED transactions can be settled");
        }
        if (this.financialStatus != FinancialStatus.RESERVED) {
            throw new IllegalStateException("Only RESERVED transactions can be settled");
        }
        this.financialStatus = FinancialStatus.SETTLED;
    }

    public void revert() {
        if (this.status != TransactionStatus.DENIED && this.status != TransactionStatus.FRAUD_CONFIRMED) {
            throw new IllegalStateException("Only DENIED or FRAUD_CONFIRMED transactions can be reverted");
        }
        if (this.financialStatus != FinancialStatus.RESERVED) {
            throw new IllegalStateException("Only RESERVED transactions can be reverted");
        }
        this.financialStatus = FinancialStatus.REVERTED;
    }

    public void assignRiskLevel(RiskLevel riskLevel) {
        this.riskLevel = riskLevel;
    }

    private void transitionTo(TransactionStatus newStatus) {
        if (!canTransitionTo(newStatus)) {
            throw new InvalidTransactionStateTransitionException(this.status, newStatus);
        }
        this.status = newStatus;
    }
}
