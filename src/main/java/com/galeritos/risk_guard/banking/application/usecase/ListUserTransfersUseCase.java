package com.galeritos.risk_guard.banking.application.usecase;

import java.util.UUID;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.galeritos.risk_guard.banking.domain.model.Transaction;
import com.galeritos.risk_guard.banking.infrastructure.persistence.repository.TransactionRepository;

@Service
public class ListUserTransfersUseCase {

    private final TransactionRepository transactionRepository;

    public ListUserTransfersUseCase(TransactionRepository transactionRepository) {
        this.transactionRepository = transactionRepository;
    }

    @Transactional(readOnly = true)
    public Page<Transaction> execute(UUID userId, int page, int size) {
        return transactionRepository.findUserTransactionsPaged(userId,
                PageRequest.of(page, size, Sort.by("createdAt").descending()));
    }
}
