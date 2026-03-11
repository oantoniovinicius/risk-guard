package com.galeritos.risk_guard.banking.domain.repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import com.galeritos.risk_guard.banking.domain.model.Transaction;
import com.galeritos.risk_guard.banking.domain.model.enums.TransactionStatus;

public interface TransactionRepository {

    Optional<Transaction> findById(UUID id);

    List<Transaction> findBySenderId(UUID senderId);

    List<Transaction> findByReceiverId(UUID receiverId);

    List<Transaction> findUserTransactions(UUID userId);

    List<Transaction> findByStatus(TransactionStatus status);

    Transaction save(Transaction transaction);

}