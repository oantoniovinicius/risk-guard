package com.galeritos.risk_guard.banking.infrastructure.persistence;

import java.util.List;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import com.galeritos.risk_guard.banking.domain.model.Transaction;
import com.galeritos.risk_guard.banking.domain.model.enums.TransactionStatus;
import com.galeritos.risk_guard.banking.domain.repository.TransactionRepository;

@Repository
public interface JpaTransactionRepository
        extends JpaRepository<Transaction, UUID>, TransactionRepository {

    List<Transaction> findBySenderId(UUID senderId);

    List<Transaction> findByReceiverId(UUID receiverId);

    List<Transaction> findByStatus(TransactionStatus status);

    @Override
    @Query("""
                SELECT t FROM Transaction t
                WHERE t.senderId = :userId
                   OR t.receiverId = :userId
                ORDER BY t.createdAt DESC
            """)
    List<Transaction> findUserTransactions(UUID userId);
}