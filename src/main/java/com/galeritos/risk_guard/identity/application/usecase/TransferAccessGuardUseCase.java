package com.galeritos.risk_guard.identity.application.usecase;

import java.util.UUID;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.galeritos.risk_guard.banking.domain.exception.TransactionNotFoundException;
import com.galeritos.risk_guard.banking.domain.model.Transaction;
import com.galeritos.risk_guard.banking.infrastructure.persistence.repository.TransactionRepository;
import com.galeritos.risk_guard.identity.application.security.AuthenticatedUser;
import com.galeritos.risk_guard.identity.application.security.CurrentUserProvider;
import com.galeritos.risk_guard.identity.domain.exception.ForbiddenTransferOperationException;
import com.galeritos.risk_guard.identity.domain.exception.UserNotActiveException;
import com.galeritos.risk_guard.identity.domain.exception.UserNotFoundException;
import com.galeritos.risk_guard.identity.domain.model.User;
import com.galeritos.risk_guard.identity.domain.model.enums.Role;
import com.galeritos.risk_guard.identity.domain.model.enums.UserStatus;
import com.galeritos.risk_guard.identity.infrastructure.persistence.repository.UserRepository;

@Service
public class TransferAccessGuardUseCase {
    private final CurrentUserProvider currentUserProvider;
    private final UserRepository userRepository;
    private final TransactionRepository transactionRepository;

    public TransferAccessGuardUseCase(
            CurrentUserProvider currentUserProvider,
            UserRepository userRepository,
            TransactionRepository transactionRepository) {
        this.currentUserProvider = currentUserProvider;
        this.userRepository = userRepository;
        this.transactionRepository = transactionRepository;
    }

    @Transactional(readOnly = true)
    public void assertCanCreateTransfer(UUID senderId) {
        AuthenticatedUser authUser = currentUserProvider.getAuthenticatedUser();
        if (authUser.role() != Role.USER) {
            throw new ForbiddenTransferOperationException();
        }

        User persistedUser = loadPersistedUser(authUser.userId());
        assertActive(persistedUser);
        if (!persistedUser.getId().equals(senderId)) {
            throw new ForbiddenTransferOperationException();
        }
    }

    @Transactional(readOnly = true)
    public void assertCanConfirmCustomerDecision(UUID transactionId) {
        AuthenticatedUser authUser = currentUserProvider.getAuthenticatedUser();
        if (authUser.role() != Role.USER) {
            throw new ForbiddenTransferOperationException();
        }

        User persistedUser = loadPersistedUser(authUser.userId());
        assertActive(persistedUser);

        Transaction transaction = transactionRepository.findById(transactionId)
                .orElseThrow(() -> new TransactionNotFoundException(transactionId));

        if (!transaction.getSenderId().equals(persistedUser.getId())) {
            throw new ForbiddenTransferOperationException();
        }
    }

    @Transactional(readOnly = true)
    public void assertCanActAsAnalyst() {
        AuthenticatedUser authUser = currentUserProvider.getAuthenticatedUser();
        if (authUser.role() != Role.ANALYST) {
            throw new ForbiddenTransferOperationException();
        }

        User persistedUser = loadPersistedUser(authUser.userId());
        assertActive(persistedUser);
    }

    private User loadPersistedUser(UUID userId) {
        return userRepository.findById(userId)
                .orElseThrow(() -> new UserNotFoundException(userId));
    }

    private void assertActive(User user) {
        if (user.getStatus() != UserStatus.ACTIVE) {
            throw new UserNotActiveException(user.getId(), user.getStatus());
        }
    }
}
