package com.galeritos.risk_guard.banking.infrastructure.persistence.repository;

import java.util.List;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

import com.galeritos.risk_guard.banking.domain.model.TransactionDecisionHistory;

public interface TransactionDecisionHistoryRepository extends JpaRepository<TransactionDecisionHistory, UUID> {

    List<TransactionDecisionHistory> findByTransactionIdOrderByCreatedAtDesc(UUID transactionId);
}
