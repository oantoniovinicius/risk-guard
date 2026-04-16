package com.galeritos.risk_guard.identity.application.usecase;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Optional;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.galeritos.risk_guard.banking.domain.model.Transaction;
import com.galeritos.risk_guard.banking.domain.model.enums.FinancialStatus;
import com.galeritos.risk_guard.banking.domain.model.enums.TransactionStatus;
import com.galeritos.risk_guard.banking.infrastructure.persistence.repository.TransactionRepository;
import com.galeritos.risk_guard.identity.application.security.AuthenticatedUser;
import com.galeritos.risk_guard.identity.application.security.CurrentUserProvider;
import com.galeritos.risk_guard.identity.domain.exception.ForbiddenTransferOperationException;
import com.galeritos.risk_guard.identity.domain.exception.UserNotActiveException;
import com.galeritos.risk_guard.identity.domain.model.User;
import com.galeritos.risk_guard.identity.domain.model.enums.Role;
import com.galeritos.risk_guard.identity.domain.model.enums.UserStatus;
import com.galeritos.risk_guard.identity.infrastructure.persistence.repository.UserRepository;

@ExtendWith(MockitoExtension.class)
class TransferAccessGuardUseCaseTest {
    @Mock
    private CurrentUserProvider currentUserProvider;

    @Mock
    private UserRepository userRepository;

    @Mock
    private TransactionRepository transactionRepository;

    private TransferAccessGuardUseCase useCase;

    @BeforeEach
    void setUp() {
        useCase = new TransferAccessGuardUseCase(currentUserProvider, userRepository, transactionRepository);
    }

    @Test
    void shouldBlockPendingUserFromCreatingTransfer() {
        UUID userId = UUID.randomUUID();
        User user = new User(null, "Pending", "pending@example.com", "12345678901", Role.USER, UserStatus.PENDING);

        when(currentUserProvider.getAuthenticatedUser()).thenReturn(authenticatedUser(userId, Role.USER));
        when(userRepository.findById(userId)).thenReturn(Optional.of(user));

        assertThrows(UserNotActiveException.class, () -> useCase.assertCanCreateTransfer(userId));
    }

    @Test
    void shouldBlockUserFromCreatingTransferForAnotherSender() {
        UUID authUserId = UUID.randomUUID();
        UUID otherSenderId = UUID.randomUUID();
        User user = new User(authUserId, "Active", "active@example.com", "12345678901", Role.USER, UserStatus.ACTIVE);

        when(currentUserProvider.getAuthenticatedUser()).thenReturn(authenticatedUser(authUserId, Role.USER));
        when(userRepository.findById(authUserId)).thenReturn(Optional.of(user));

        assertThrows(ForbiddenTransferOperationException.class, () -> useCase.assertCanCreateTransfer(otherSenderId));
    }

    @Test
    void shouldAllowActiveSenderToConfirmOwnTransaction() {
        UUID senderId = UUID.randomUUID();
        UUID transactionId = UUID.randomUUID();
        User user = new User(senderId, "Sender", "sender@example.com", "12345678901", Role.USER, UserStatus.ACTIVE);
        Transaction transaction = new Transaction(
                senderId,
                UUID.randomUUID(),
                BigDecimal.TEN,
                TransactionStatus.AWAITING_CUSTOMER,
                FinancialStatus.RESERVED,
                null,
                LocalDateTime.now());

        when(currentUserProvider.getAuthenticatedUser()).thenReturn(authenticatedUser(senderId, Role.USER));
        when(userRepository.findById(senderId)).thenReturn(Optional.of(user));
        when(transactionRepository.findById(transactionId)).thenReturn(Optional.of(transaction));

        assertDoesNotThrow(() -> useCase.assertCanConfirmCustomerDecision(transactionId));
    }

    private AuthenticatedUser authenticatedUser(UUID userId, Role role) {
        return new AuthenticatedUser(
                userId,
                "test@example.com",
                role,
                java.util.List.of(new org.springframework.security.core.authority.SimpleGrantedAuthority(
                        "ROLE_" + role.name())));
    }
}
