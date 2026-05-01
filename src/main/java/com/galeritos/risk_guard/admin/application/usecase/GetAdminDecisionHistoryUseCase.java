package com.galeritos.risk_guard.admin.application.usecase;

import java.util.UUID;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.galeritos.risk_guard.admin.domain.model.AdminDecisionHistory;
import com.galeritos.risk_guard.admin.infrastructure.persistence.repository.AdminDecisionHistoryRepository;

@Service
public class GetAdminDecisionHistoryUseCase {
    private final AdminDecisionHistoryRepository repository;

    public GetAdminDecisionHistoryUseCase(AdminDecisionHistoryRepository repository) {
        this.repository = repository;
    }

    @Transactional(readOnly = true)
    public Page<AdminDecisionHistory> execute(UUID targetUserId, int page, int size) {
        Pageable pageable = PageRequest.of(page, size);
        if (targetUserId != null) {
            return repository.findByTargetUserIdOrderByCreatedAtDesc(targetUserId, pageable);
        }

        return repository.findAllByOrderByCreatedAtDesc(pageable);
    }
}
