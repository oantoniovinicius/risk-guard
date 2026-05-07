package com.galeritos.risk_guard.identity.application.usecase;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.when;

import java.util.Optional;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.galeritos.risk_guard.admin.application.usecase.RecordAdminDecisionUseCase;
import com.galeritos.risk_guard.identity.domain.exception.UserNotFoundException;
import com.galeritos.risk_guard.identity.domain.model.User;
import com.galeritos.risk_guard.identity.domain.model.enums.Role;
import com.galeritos.risk_guard.identity.domain.model.enums.UserStatus;
import com.galeritos.risk_guard.identity.infrastructure.persistence.repository.UserRepository;

@ExtendWith(MockitoExtension.class)
class DenyUserUseCaseTest {
    @Mock
    private UserRepository userRepository;

    @Mock
    private RecordAdminDecisionUseCase recordAdminDecisionUseCase;

    private DenyUserUseCase useCase;

    @BeforeEach
    void setUp() {
        useCase = new DenyUserUseCase(userRepository, recordAdminDecisionUseCase);
    }

    @Test
    void shouldDenyPendingUser() {
        UUID userId = UUID.randomUUID();
        User pendingUser = new User(userId, "Pending", "pending@example.com", "12345678901", Role.USER, UserStatus.PENDING);

        when(userRepository.findByIdForUpdate(userId)).thenReturn(Optional.of(pendingUser));
        when(userRepository.save(pendingUser)).thenReturn(pendingUser);

        User denied = useCase.execute(userId);
        assertEquals(UserStatus.REJECTED, denied.getStatus());
    }

    @Test
    void shouldFailWhenUserDoesNotExist() {
        UUID userId = UUID.randomUUID();
        when(userRepository.findByIdForUpdate(userId)).thenReturn(Optional.empty());

        assertThrows(UserNotFoundException.class, () -> useCase.execute(userId));
    }
}
