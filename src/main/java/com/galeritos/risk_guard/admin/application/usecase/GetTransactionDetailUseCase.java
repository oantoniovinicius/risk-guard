package com.galeritos.risk_guard.admin.application.usecase;

import java.util.UUID;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.galeritos.risk_guard.banking.domain.exception.TransactionNotFoundException;
import com.galeritos.risk_guard.banking.domain.model.Transaction;
import com.galeritos.risk_guard.banking.infrastructure.persistence.repository.TransactionRepository;

@Service
public class GetTransactionDetailUseCase {

    private final TransactionRepository repository;

    public GetTransactionDetailUseCase(TransactionRepository repository) {
        this.repository = repository;
    }

    @Transactional(readOnly = true)
    public Transaction execute(UUID transactionId) {
        return repository.findById(transactionId)
                .orElseThrow(() -> new TransactionNotFoundException(transactionId));
    }
}
