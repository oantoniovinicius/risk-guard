package com.galeritos.risk_guard.identity.application.usecase;

import java.util.UUID;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import com.galeritos.risk_guard.identity.application.event.UserApprovedEvent;
import com.galeritos.risk_guard.identity.application.port.out.IdentityEventPublisher;
import com.galeritos.risk_guard.identity.domain.exception.UserNotFoundException;
import com.galeritos.risk_guard.identity.domain.model.User;
import com.galeritos.risk_guard.identity.infrastructure.persistence.repository.UserRepository;

@Service
public class ApproveUserUseCase {
    private final UserRepository userRepository;
    private final IdentityEventPublisher identityEventPublisher;

    public ApproveUserUseCase(UserRepository userRepository, IdentityEventPublisher identityEventPublisher) {
        this.userRepository = userRepository;
        this.identityEventPublisher = identityEventPublisher;
    }

    @Transactional
    public User execute(UUID userId) {
        User user = userRepository.findByIdForUpdate(userId)
                .orElseThrow(() -> new UserNotFoundException(userId));

        user.activate();
        user = userRepository.save(user);
        publishUserApproved(UserApprovedEvent.from(user));
        return user;
    }

    private void publishUserApproved(UserApprovedEvent event) {
        if (!TransactionSynchronizationManager.isSynchronizationActive()) {
            identityEventPublisher.publishUserApproved(event);
            return;
        }

        TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
            @Override
            public void afterCommit() {
                identityEventPublisher.publishUserApproved(event);
            }
        });
    }
}
