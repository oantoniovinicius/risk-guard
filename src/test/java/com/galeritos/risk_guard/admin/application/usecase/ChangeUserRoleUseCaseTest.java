package com.galeritos.risk_guard.admin.application.usecase;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Optional;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.galeritos.risk_guard.admin.domain.exception.InvalidRoleChangeException;
import com.galeritos.risk_guard.identity.domain.model.User;
import com.galeritos.risk_guard.identity.domain.model.enums.Role;
import com.galeritos.risk_guard.identity.domain.model.enums.UserStatus;
import com.galeritos.risk_guard.identity.infrastructure.persistence.repository.UserRepository;

@ExtendWith(MockitoExtension.class)
class ChangeUserRoleUseCaseTest {
    @Mock
    private UserRepository userRepository;

    @Mock
    private RecordAdminDecisionUseCase recordAdminDecisionUseCase;

    private ChangeUserRoleUseCase useCase;

    @BeforeEach
    void setUp() {
        useCase = new ChangeUserRoleUseCase(userRepository, recordAdminDecisionUseCase);
    }

    @Test
    void shouldChangeRoleAndRecordDecision() {
        UUID userId = UUID.randomUUID();
        User user = new User(userId, "User", "role.change@example.com", "12345678901", Role.USER, UserStatus.ACTIVE);

        when(userRepository.findByIdForUpdate(userId)).thenReturn(Optional.of(user));
        when(userRepository.save(user)).thenReturn(user);

        User updated = useCase.execute(userId, Role.ANALYST);

        assertEquals(Role.ANALYST, updated.getRole());
        verify(recordAdminDecisionUseCase).recordRoleChange(user, Role.USER, Role.ANALYST);
    }

    @Test
    void shouldFailWhenRequestedRoleEqualsCurrentRole() {
        UUID userId = UUID.randomUUID();
        User user = new User(userId, "Admin", "admin.same.role@example.com", "12345678902", Role.ADMIN, UserStatus.ACTIVE);

        when(userRepository.findByIdForUpdate(userId)).thenReturn(Optional.of(user));

        assertThrows(InvalidRoleChangeException.class, () -> useCase.execute(userId, Role.ADMIN));
    }
}
