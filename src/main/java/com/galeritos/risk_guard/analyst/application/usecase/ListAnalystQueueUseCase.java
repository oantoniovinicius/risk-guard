package com.galeritos.risk_guard.analyst.application.usecase;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;

import com.galeritos.risk_guard.banking.domain.model.Transaction;
import com.galeritos.risk_guard.banking.domain.model.enums.TransactionStatus;
import com.galeritos.risk_guard.banking.infrastructure.persistence.repository.TransactionRepository;
import com.galeritos.risk_guard.shared.enums.RiskLevel;

@Service
public class ListAnalystQueueUseCase {

    private final TransactionRepository transactionRepository;

    public ListAnalystQueueUseCase(TransactionRepository transactionRepository) {
        this.transactionRepository = transactionRepository;
    }

    public Page<Transaction> execute(RiskLevel riskLevel, LocalDateTime from, LocalDateTime to, int page, int size) {
        List<Specification<Transaction>> specs = new ArrayList<>();

        specs.add((root, q, cb) -> root.get("status").in(TransactionStatus.AWAITING_ANALYST, TransactionStatus.DISPUTED));

        if (riskLevel != null) {
            specs.add((root, q, cb) -> cb.equal(root.get("riskLevel"), riskLevel));
        }
        if (from != null) {
            specs.add((root, q, cb) -> cb.greaterThanOrEqualTo(root.get("createdAt"), from));
        }
        if (to != null) {
            specs.add((root, q, cb) -> cb.lessThanOrEqualTo(root.get("createdAt"), to));
        }

        // oldest first — the longest-waiting transaction is the most urgent
        PageRequest pageable = PageRequest.of(page, size, Sort.by("createdAt").ascending());
        return transactionRepository.findAll(Specification.allOf(specs), pageable);
    }
}
