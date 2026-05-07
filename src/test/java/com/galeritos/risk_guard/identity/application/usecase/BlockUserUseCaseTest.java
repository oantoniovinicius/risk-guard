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
class BlockUserUseCaseTest {
    @Mock
    private UserRepository userRepository;

    @Mock
    private RecordAdminDecisionUseCase recordAdminDecisionUseCase;

    private BlockUserUseCase useCase;

    @BeforeEach
    void setUp() {
        useCase = new BlockUserUseCase(userRepository, recordAdminDecisionUseCase);
    }

    @Test
    void shouldBlockActiveUser() {
        UUID userId = UUID.randomUUID();
        User activeUser = new User(userId, "Active", "active.block@example.com", "12345678931", Role.USER, UserStatus.ACTIVE);

        when(userRepository.findByIdForUpdate(userId)).thenReturn(Optional.of(activeUser));
        when(userRepository.save(activeUser)).thenReturn(activeUser);

        User blocked = useCase.execute(userId);
        assertEquals(UserStatus.BLOCKED, blocked.getStatus());
    }

    @Test
    void shouldFailWhenUserDoesNotExist() {
        UUID userId = UUID.randomUUID();
        when(userRepository.findByIdForUpdate(userId)).thenReturn(Optional.empty());

        assertThrows(UserNotFoundException.class, () -> useCase.execute(userId));
    }
}
