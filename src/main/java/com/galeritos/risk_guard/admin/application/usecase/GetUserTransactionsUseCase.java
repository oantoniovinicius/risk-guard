package com.galeritos.risk_guard.admin.application.usecase;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.galeritos.risk_guard.banking.domain.model.Transaction;
import com.galeritos.risk_guard.banking.domain.model.enums.TransactionStatus;
import com.galeritos.risk_guard.banking.infrastructure.persistence.repository.TransactionRepository;
import com.galeritos.risk_guard.identity.domain.exception.UserNotFoundException;
import com.galeritos.risk_guard.identity.infrastructure.persistence.repository.UserRepository;
import com.galeritos.risk_guard.shared.enums.RiskLevel;

@Service
public class GetUserTransactionsUseCase {

    private final TransactionRepository transactionRepository;
    private final UserRepository userRepository;

    public GetUserTransactionsUseCase(TransactionRepository transactionRepository, UserRepository userRepository) {
        this.transactionRepository = transactionRepository;
        this.userRepository = userRepository;
    }

    @Transactional(readOnly = true)
    public Page<Transaction> execute(UUID userId, TransactionStatus status, RiskLevel riskLevel,
            LocalDateTime from, LocalDateTime to, int page, int size) {
        if (!userRepository.existsById(userId)) {
            throw new UserNotFoundException(userId);
        }

        List<Specification<Transaction>> specs = new ArrayList<>();
        specs.add((root, q, cb) -> cb.or(
                cb.equal(root.get("senderId"), userId),
                cb.equal(root.get("receiverId"), userId)));
        if (status != null) specs.add((root, q, cb) -> cb.equal(root.get("status"), status));
        if (riskLevel != null) specs.add((root, q, cb) -> cb.equal(root.get("riskLevel"), riskLevel));
        if (from != null) specs.add((root, q, cb) -> cb.greaterThanOrEqualTo(root.get("createdAt"), from));
        if (to != null) specs.add((root, q, cb) -> cb.lessThanOrEqualTo(root.get("createdAt"), to));

        return transactionRepository.findAll(Specification.allOf(specs),
                PageRequest.of(page, size, Sort.by("createdAt").descending()));
    }
}
