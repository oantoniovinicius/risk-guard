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
class UnsuspendUserUseCaseTest {
    @Mock
    private UserRepository userRepository;

    @Mock
    private RecordAdminDecisionUseCase recordAdminDecisionUseCase;

    private UnsuspendUserUseCase useCase;

    @BeforeEach
    void setUp() {
        useCase = new UnsuspendUserUseCase(userRepository, recordAdminDecisionUseCase);
    }

    @Test
    void shouldUnsuspendSuspendedUser() {
        UUID userId = UUID.randomUUID();
        User suspendedUser = new User(userId, "Suspended", "rejected.unsuspend@example.com", "12345678932", Role.USER,
                UserStatus.SUSPENDED);

        when(userRepository.findByIdForUpdate(userId)).thenReturn(Optional.of(suspendedUser));
        when(userRepository.save(suspendedUser)).thenReturn(suspendedUser);

        User unsuspended = useCase.execute(userId);
        assertEquals(UserStatus.ACTIVE, unsuspended.getStatus());
    }

    @Test
    void shouldFailWhenUserDoesNotExist() {
        UUID userId = UUID.randomUUID();
        when(userRepository.findByIdForUpdate(userId)).thenReturn(Optional.empty());

        assertThrows(UserNotFoundException.class, () -> useCase.execute(userId));
    }
}
