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
class UnblockUserUseCaseTest {
    @Mock
    private UserRepository userRepository;

    @Mock
    private RecordAdminDecisionUseCase recordAdminDecisionUseCase;

    private UnblockUserUseCase useCase;

    @BeforeEach
    void setUp() {
        useCase = new UnblockUserUseCase(userRepository, recordAdminDecisionUseCase);
    }

    @Test
    void shouldUnblockBlockedUser() {
        UUID userId = UUID.randomUUID();
        User blockedUser = new User(userId, "Blocked", "blocked.unblock@example.com", "12345678933", Role.USER,
                UserStatus.BLOCKED);

        when(userRepository.findByIdForUpdate(userId)).thenReturn(Optional.of(blockedUser));
        when(userRepository.save(blockedUser)).thenReturn(blockedUser);

        User unblocked = useCase.execute(userId);
        assertEquals(UserStatus.ACTIVE, unblocked.getStatus());
    }

    @Test
    void shouldFailWhenUserDoesNotExist() {
        UUID userId = UUID.randomUUID();
        when(userRepository.findByIdForUpdate(userId)).thenReturn(Optional.empty());

        assertThrows(UserNotFoundException.class, () -> useCase.execute(userId));
    }
}
