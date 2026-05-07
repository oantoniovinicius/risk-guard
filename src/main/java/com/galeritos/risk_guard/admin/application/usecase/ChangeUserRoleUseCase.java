package com.galeritos.risk_guard.admin.application.usecase;

import java.util.UUID;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.galeritos.risk_guard.admin.domain.exception.InvalidRoleChangeException;
import com.galeritos.risk_guard.identity.domain.exception.UserNotFoundException;
import com.galeritos.risk_guard.identity.domain.model.User;
import com.galeritos.risk_guard.identity.domain.model.enums.Role;
import com.galeritos.risk_guard.identity.infrastructure.persistence.repository.UserRepository;

@Service
public class ChangeUserRoleUseCase {
    private final UserRepository userRepository;
    private final RecordAdminDecisionUseCase recordAdminDecisionUseCase;

    public ChangeUserRoleUseCase(
            UserRepository userRepository,
            RecordAdminDecisionUseCase recordAdminDecisionUseCase) {
        this.userRepository = userRepository;
        this.recordAdminDecisionUseCase = recordAdminDecisionUseCase;
    }

    @Transactional
    public User execute(UUID userId, Role requestedRole) {
        User user = userRepository.findByIdForUpdate(userId)
                .orElseThrow(() -> new UserNotFoundException(userId));

        Role currentRole = user.getRole();
        if (currentRole == requestedRole) {
            throw new InvalidRoleChangeException(userId, currentRole, requestedRole);
        }

        user.changeRole(requestedRole);
        user = userRepository.save(user);
        recordAdminDecisionUseCase.recordRoleChange(user, currentRole, requestedRole);
        return user;
    }
}
