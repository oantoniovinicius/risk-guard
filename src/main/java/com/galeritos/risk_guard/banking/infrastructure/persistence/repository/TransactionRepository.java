package com.galeritos.risk_guard.banking.infrastructure.persistence.repository;

import java.util.UUID;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;

import com.galeritos.risk_guard.banking.domain.model.Transaction;
import java.util.List;
import com.galeritos.risk_guard.banking.domain.model.enums.TransactionStatus;
import com.galeritos.risk_guard.banking.domain.model.enums.FinancialStatus;

import jakarta.persistence.LockModeType;

public interface TransactionRepository extends JpaRepository<Transaction, UUID> {

    List<Transaction> findBySenderId(UUID senderId);

    List<Transaction> findByReceiverId(UUID receiverId);

    List<Transaction> findByStatus(TransactionStatus status);

    List<Transaction> findByFinancialStatus(FinancialStatus financialStatus);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("""
                SELECT t FROM Transaction t
                WHERE t.id = :transactionId
            """)
    Optional<Transaction> findByIdForUpdate(UUID transactionId);

    @Query("""
                SELECT t FROM Transaction t
                WHERE t.senderId = :userId
                   OR t.receiverId = :userId
                ORDER BY t.createdAt DESC
            """)
    List<Transaction> findUserTransactions(UUID userId);
}
