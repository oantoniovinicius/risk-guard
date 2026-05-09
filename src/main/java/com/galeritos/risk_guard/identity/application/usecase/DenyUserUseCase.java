package com.galeritos.risk_guard.identity.application.usecase;

import java.util.UUID;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.galeritos.risk_guard.admin.application.usecase.RecordAdminDecisionUseCase;
import com.galeritos.risk_guard.admin.domain.model.enums.AdminDecisionAction;
import com.galeritos.risk_guard.identity.domain.exception.UserNotFoundException;
import com.galeritos.risk_guard.identity.domain.model.User;
import com.galeritos.risk_guard.identity.domain.model.enums.UserStatus;
import com.galeritos.risk_guard.identity.infrastructure.persistence.repository.UserRepository;

@Service
public class DenyUserUseCase {

    static final int MAX_REJECTION_COUNT = 3;

    private final UserRepository userRepository;
    private final RecordAdminDecisionUseCase recordAdminDecisionUseCase;

    public DenyUserUseCase(
            UserRepository userRepository,
            RecordAdminDecisionUseCase recordAdminDecisionUseCase) {
        this.userRepository = userRepository;
        this.recordAdminDecisionUseCase = recordAdminDecisionUseCase;
    }

    @Transactional
    public User execute(UUID userId) {
        User user = userRepository.findByIdForUpdate(userId)
                .orElseThrow(() -> new UserNotFoundException(userId));

        UserStatus fromStatus = user.getStatus();

        if (user.getRejectionCount() + 1 >= MAX_REJECTION_COUNT) {
            user.rejectAndBlock();
        } else {
            user.reject();
        }

        user = userRepository.save(user);
        recordAdminDecisionUseCase.recordStatusChange(
                AdminDecisionAction.DENY_USER,
                user,
                fromStatus,
                user.getStatus());
        return user;
    }
}
