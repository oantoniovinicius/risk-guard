package com.galeritos.risk_guard.identity.application.usecase;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import com.galeritos.risk_guard.identity.application.usecase.dto.RegisterUserCommand;
import com.galeritos.risk_guard.identity.domain.exception.EmailAlreadyInUseException;
import com.galeritos.risk_guard.identity.domain.exception.InvalidRegistrationRoleException;
import com.galeritos.risk_guard.identity.domain.model.User;
import com.galeritos.risk_guard.identity.domain.model.enums.Role;
import com.galeritos.risk_guard.identity.domain.model.enums.UserStatus;
import com.galeritos.risk_guard.identity.infrastructure.persistence.repository.UserCredentialRepository;
import com.galeritos.risk_guard.identity.infrastructure.persistence.repository.UserRepository;

@ExtendWith(MockitoExtension.class)
class RegisterUserUseCaseTest {
    @Mock
    private UserRepository userRepository;

    @Mock
    private UserCredentialRepository userCredentialRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    private RegisterUserUseCase useCase;

    @BeforeEach
    void setUp() {
        useCase = new RegisterUserUseCase(userRepository, userCredentialRepository, passwordEncoder);
    }

    @Test
    void shouldRegisterPendingUserWithHashedPassword() {
        RegisterUserCommand command = new RegisterUserCommand(
                "Alice",
                "alice@example.com",
                "12345678901",
                "StrongPass123",
                Role.USER);

        when(userRepository.existsByEmail(command.email())).thenReturn(false);
        when(userRepository.existsByDocument(command.document())).thenReturn(false);
        when(passwordEncoder.encode(command.password())).thenReturn("hash");
        when(userRepository.save(any(User.class))).thenAnswer(invocation -> invocation.getArgument(0));

        User user = useCase.execute(command);

        assertEquals(UserStatus.PENDING, user.getStatus());
        verify(userCredentialRepository).save(any());
    }

    @Test
    void shouldFailWhenEmailAlreadyExists() {
        RegisterUserCommand command = new RegisterUserCommand(
                "Alice",
                "alice@example.com",
                "12345678901",
                "StrongPass123",
                Role.USER);

        when(userRepository.existsByEmail(command.email())).thenReturn(true);

        assertThrows(EmailAlreadyInUseException.class, () -> useCase.execute(command));
    }

    @Test
    void shouldFailWhenRegistrationRoleIsNotUser() {
        RegisterUserCommand command = new RegisterUserCommand(
                "Admin",
                "admin@example.com",
                "12345678901",
                "StrongPass123",
                Role.ADMIN);

        assertThrows(InvalidRegistrationRoleException.class, () -> useCase.execute(command));
    }
}
