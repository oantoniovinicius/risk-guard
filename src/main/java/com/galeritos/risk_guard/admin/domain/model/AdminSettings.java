package com.galeritos.risk_guard.admin.domain.model;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.UUID;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Getter;

@Entity
@Getter
@Table(name = "admin_settings")
public class AdminSettings {
    @Id
    private UUID id;

    @Column(name = "medium_risk_threshold", nullable = false, precision = 5, scale = 4)
    private BigDecimal mediumRiskThreshold;

    @Column(name = "high_risk_threshold", nullable = false, precision = 5, scale = 4)
    private BigDecimal highRiskThreshold;

    @Column(name = "business_start_time", nullable = false)
    private LocalTime businessStartTime;

    @Column(name = "business_end_time", nullable = false)
    private LocalTime businessEndTime;

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    protected AdminSettings() {
    }

    public AdminSettings(
            UUID id,
            BigDecimal mediumRiskThreshold,
            BigDecimal highRiskThreshold,
            LocalTime businessStartTime,
            LocalTime businessEndTime) {
        this.id = id;
        this.mediumRiskThreshold = mediumRiskThreshold;
        this.highRiskThreshold = highRiskThreshold;
        this.businessStartTime = businessStartTime;
        this.businessEndTime = businessEndTime;
        this.updatedAt = LocalDateTime.now();
    }

    public void update(
            BigDecimal mediumRiskThreshold,
            BigDecimal highRiskThreshold,
            LocalTime businessStartTime,
            LocalTime businessEndTime) {
        this.mediumRiskThreshold = mediumRiskThreshold;
        this.highRiskThreshold = highRiskThreshold;
        this.businessStartTime = businessStartTime;
        this.businessEndTime = businessEndTime;
        this.updatedAt = LocalDateTime.now();
    }
}
