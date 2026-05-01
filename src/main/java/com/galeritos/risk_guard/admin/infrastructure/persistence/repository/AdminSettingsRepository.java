package com.galeritos.risk_guard.admin.infrastructure.persistence.repository;

import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

import com.galeritos.risk_guard.admin.domain.model.AdminSettings;

public interface AdminSettingsRepository extends JpaRepository<AdminSettings, UUID> {
}
