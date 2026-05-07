package com.galeritos.risk_guard.admin.application.usecase;

import java.util.UUID;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.galeritos.risk_guard.banking.domain.model.Transaction;
import com.galeritos.risk_guard.banking.infrastructure.persistence.repository.TransactionRepository;
import com.galeritos.risk_guard.identity.domain.exception.UserNotFoundException;
import com.galeritos.risk_guard.identity.infrastructure.persistence.repository.UserRepository;

@Service
public class GetUserTransactionsUseCase {

    private final TransactionRepository transactionRepository;
    private final UserRepository userRepository;

    public GetUserTransactionsUseCase(TransactionRepository transactionRepository, UserRepository userRepository) {
        this.transactionRepository = transactionRepository;
        this.userRepository = userRepository;
    }

    @Transactional(readOnly = true)
    public Page<Transaction> execute(UUID userId, int page, int size) {
        if (!userRepository.existsById(userId)) {
            throw new UserNotFoundException(userId);
        }
        return transactionRepository.findUserTransactionsPaged(userId,
                PageRequest.of(page, size, Sort.by("createdAt").descending()));
    }
}
