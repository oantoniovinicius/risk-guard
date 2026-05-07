package com.galeritos.risk_guard.admin.application.usecase;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;

import com.galeritos.risk_guard.identity.domain.model.User;
import com.galeritos.risk_guard.identity.domain.model.enums.Role;
import com.galeritos.risk_guard.identity.domain.model.enums.UserStatus;
import com.galeritos.risk_guard.identity.infrastructure.persistence.repository.UserRepository;

@ExtendWith(MockitoExtension.class)
class ListUsersUseCaseTest {

    @Mock
    private UserRepository repository;

    private ListUsersUseCase useCase;

    @BeforeEach
    void setUp() {
        useCase = new ListUsersUseCase(repository);
    }

    private User user(UserStatus status, Role role) {
        return new User(UUID.randomUUID(), "Test User", "test@example.com", "12345678901", role, status);
    }

    @Test
    @SuppressWarnings("unchecked")
    void shouldReturnAllUsersWhenNoFiltersProvided() {
        List<User> users = List.of(
                user(UserStatus.ACTIVE, Role.USER),
                user(UserStatus.PENDING, Role.USER));
        when(repository.findAll(any(Specification.class), any(Pageable.class))).thenReturn(new PageImpl<>(users));

        Page<User> result = useCase.execute(null, null, 0, 20);

        assertEquals(2, result.getTotalElements());
        verify(repository).findAll(any(Specification.class), any(Pageable.class));
    }

    @Test
    @SuppressWarnings("unchecked")
    void shouldFilterByStatus() {
        List<User> users = List.of(user(UserStatus.ACTIVE, Role.USER));
        when(repository.findAll(any(Specification.class), any(Pageable.class))).thenReturn(new PageImpl<>(users));

        Page<User> result = useCase.execute(UserStatus.ACTIVE, null, 0, 20);

        assertEquals(1, result.getTotalElements());
        assertEquals(UserStatus.ACTIVE, result.getContent().get(0).getStatus());
    }

    @Test
    @SuppressWarnings("unchecked")
    void shouldFilterByRole() {
        List<User> users = List.of(user(UserStatus.ACTIVE, Role.ANALYST));
        when(repository.findAll(any(Specification.class), any(Pageable.class))).thenReturn(new PageImpl<>(users));

        Page<User> result = useCase.execute(null, Role.ANALYST, 0, 20);

        assertEquals(1, result.getTotalElements());
        assertEquals(Role.ANALYST, result.getContent().get(0).getRole());
    }

    @Test
    @SuppressWarnings("unchecked")
    void shouldReturnEmptyPageWhenNoMatches() {
        when(repository.findAll(any(Specification.class), any(Pageable.class))).thenReturn(Page.empty());

        Page<User> result = useCase.execute(UserStatus.BLOCKED, Role.ADMIN, 0, 20);

        assertEquals(0, result.getTotalElements());
    }
}
