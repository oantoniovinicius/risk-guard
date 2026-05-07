package com.galeritos.risk_guard.banking.infrastructure.persistence.repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;

import com.galeritos.risk_guard.banking.domain.model.Transaction;
import com.galeritos.risk_guard.banking.domain.model.enums.FinancialStatus;
import com.galeritos.risk_guard.banking.domain.model.enums.TransactionStatus;

import jakarta.persistence.LockModeType;

public interface TransactionRepository extends JpaRepository<Transaction, UUID>, JpaSpecificationExecutor<Transaction> {

    List<Transaction> findBySenderId(UUID senderId);

    List<Transaction> findByReceiverId(UUID receiverId);

    List<Transaction> findByStatus(TransactionStatus status);

    List<Transaction> findByFinancialStatus(FinancialStatus financialStatus);

    long countByStatus(TransactionStatus status);

    @Query("""
                SELECT t.id FROM Transaction t
                WHERE t.status = com.galeritos.risk_guard.banking.domain.model.enums.TransactionStatus.AWAITING_CUSTOMER
                  AND t.customerDecisionDeadlineAt IS NOT NULL
                  AND t.customerDecisionDeadlineAt <= :now
            """)
    List<UUID> findExpiredAwaitingCustomerIds(LocalDateTime now);

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

    @Query("""
                SELECT t FROM Transaction t
                WHERE t.senderId = :userId
                   OR t.receiverId = :userId
            """)
    Page<Transaction> findUserTransactionsPaged(UUID userId, Pageable pageable);

    Page<Transaction> findAll(Specification<Transaction> spec, Pageable pageable);
}
