package com.galeritos.risk_guard.admin.application.usecase;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.galeritos.risk_guard.banking.domain.model.Transaction;
import com.galeritos.risk_guard.banking.domain.model.enums.TransactionStatus;
import com.galeritos.risk_guard.banking.infrastructure.persistence.repository.TransactionRepository;
import com.galeritos.risk_guard.shared.enums.RiskLevel;

@Service
public class ListTransactionsUseCase {

    private final TransactionRepository repository;

    public ListTransactionsUseCase(TransactionRepository repository) {
        this.repository = repository;
    }

    @Transactional(readOnly = true)
    public Page<Transaction> execute(TransactionStatus status, RiskLevel riskLevel,
            LocalDateTime from, LocalDateTime to, int page, int size) {
        List<Specification<Transaction>> specs = new ArrayList<>();
        if (status != null) specs.add((root, q, cb) -> cb.equal(root.get("status"), status));
        if (riskLevel != null) specs.add((root, q, cb) -> cb.equal(root.get("riskLevel"), riskLevel));
        if (from != null) specs.add((root, q, cb) -> cb.greaterThanOrEqualTo(root.get("createdAt"), from));
        if (to != null) specs.add((root, q, cb) -> cb.lessThanOrEqualTo(root.get("createdAt"), to));

        Specification<Transaction> combined = Specification.allOf(specs);
        return repository.findAll(combined, PageRequest.of(page, size, Sort.by("createdAt").descending()));
    }
}
