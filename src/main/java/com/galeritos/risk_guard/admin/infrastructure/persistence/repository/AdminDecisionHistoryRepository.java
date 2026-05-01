package com.galeritos.risk_guard.admin.infrastructure.persistence.repository;

import java.util.UUID;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import com.galeritos.risk_guard.admin.domain.model.AdminDecisionHistory;

public interface AdminDecisionHistoryRepository extends JpaRepository<AdminDecisionHistory, UUID> {
    Page<AdminDecisionHistory> findByTargetUserIdOrderByCreatedAtDesc(UUID targetUserId, Pageable pageable);

    Page<AdminDecisionHistory> findAllByOrderByCreatedAtDesc(Pageable pageable);
}
