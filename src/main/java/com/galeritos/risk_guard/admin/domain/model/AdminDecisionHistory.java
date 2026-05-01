package com.galeritos.risk_guard.admin.domain.model;

import java.time.LocalDateTime;
import java.util.UUID;

import com.galeritos.risk_guard.admin.domain.model.enums.AdminDecisionAction;
import com.galeritos.risk_guard.identity.domain.model.enums.Role;
import com.galeritos.risk_guard.identity.domain.model.enums.UserStatus;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Getter;

@Entity
@Getter
@Table(name = "admin_decision_history")
public class AdminDecisionHistory {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "actor_user_id", nullable = false)
    private UUID actorUserId;

    @Enumerated(EnumType.STRING)
    @Column(name = "actor_role", nullable = false)
    private Role actorRole;

    @Column(name = "target_user_id", nullable = false)
    private UUID targetUserId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private AdminDecisionAction action;

    @Enumerated(EnumType.STRING)
    @Column(name = "from_status")
    private UserStatus fromStatus;

    @Enumerated(EnumType.STRING)
    @Column(name = "to_status")
    private UserStatus toStatus;

    @Enumerated(EnumType.STRING)
    @Column(name = "from_role")
    private Role fromRole;

    @Enumerated(EnumType.STRING)
    @Column(name = "to_role")
    private Role toRole;

    @Column(length = 500)
    private String reason;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    protected AdminDecisionHistory() {
    }

    public AdminDecisionHistory(
            UUID actorUserId,
            Role actorRole,
            UUID targetUserId,
            AdminDecisionAction action,
            UserStatus fromStatus,
            UserStatus toStatus,
            Role fromRole,
            Role toRole,
            String reason) {
        this.actorUserId = actorUserId;
        this.actorRole = actorRole;
        this.targetUserId = targetUserId;
        this.action = action;
        this.fromStatus = fromStatus;
        this.toStatus = toStatus;
        this.fromRole = fromRole;
        this.toRole = toRole;
        this.reason = reason;
        this.createdAt = LocalDateTime.now();
    }
}
