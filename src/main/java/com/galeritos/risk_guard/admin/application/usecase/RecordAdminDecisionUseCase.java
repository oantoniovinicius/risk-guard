package com.galeritos.risk_guard.admin.application.usecase;

import java.util.UUID;

import org.springframework.stereotype.Service;

import com.galeritos.risk_guard.admin.domain.model.AdminDecisionHistory;
import com.galeritos.risk_guard.admin.domain.model.enums.AdminDecisionAction;
import com.galeritos.risk_guard.admin.infrastructure.persistence.repository.AdminDecisionHistoryRepository;
import com.galeritos.risk_guard.identity.application.security.AuthenticatedUser;
import com.galeritos.risk_guard.identity.application.security.CurrentUserProvider;
import com.galeritos.risk_guard.identity.domain.model.User;
import com.galeritos.risk_guard.identity.domain.model.enums.Role;
import com.galeritos.risk_guard.identity.domain.model.enums.UserStatus;

@Service
public class RecordAdminDecisionUseCase {
    private final AdminDecisionHistoryRepository repository;
    private final CurrentUserProvider currentUserProvider;

    public RecordAdminDecisionUseCase(
            AdminDecisionHistoryRepository repository,
            CurrentUserProvider currentUserProvider) {
        this.repository = repository;
        this.currentUserProvider = currentUserProvider;
    }

    public void recordStatusChange(AdminDecisionAction action, User target, UserStatus fromStatus, UserStatus toStatus) {
        AuthenticatedUser actor = currentUserProvider.getAuthenticatedUser();
        repository.save(new AdminDecisionHistory(
                actor.userId(),
                actor.role(),
                target.getId(),
                action,
                fromStatus,
                toStatus,
                null,
                null,
                null));
    }

    public void recordRoleChange(User target, Role fromRole, Role toRole) {
        AuthenticatedUser actor = currentUserProvider.getAuthenticatedUser();
        repository.save(new AdminDecisionHistory(
                actor.userId(),
                actor.role(),
                target.getId(),
                AdminDecisionAction.CHANGE_USER_ROLE,
                null,
                null,
                fromRole,
                toRole,
                null));
    }

    public void recordManual(
            UUID actorUserId,
            Role actorRole,
            UUID targetUserId,
            AdminDecisionAction action,
            UserStatus fromStatus,
            UserStatus toStatus,
            Role fromRole,
            Role toRole,
            String reason) {
        repository.save(new AdminDecisionHistory(
                actorUserId,
                actorRole,
                targetUserId,
                action,
                fromStatus,
                toStatus,
                fromRole,
                toRole,
                reason));
    }
}
