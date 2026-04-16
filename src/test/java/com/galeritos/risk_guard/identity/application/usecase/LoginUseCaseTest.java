package com.galeritos.risk_guard.identity.application.usecase;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.when;

import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import com.galeritos.risk_guard.identity.application.security.JwtService;
import com.galeritos.risk_guard.identity.application.usecase.dto.LoginCommand;
import com.galeritos.risk_guard.identity.application.usecase.dto.LoginResult;
import com.galeritos.risk_guard.identity.domain.exception.InvalidCredentialsException;
import com.galeritos.risk_guard.identity.domain.model.User;
import com.galeritos.risk_guard.identity.domain.model.UserCredential;
import com.galeritos.risk_guard.identity.domain.model.enums.Role;
import com.galeritos.risk_guard.identity.domain.model.enums.UserStatus;
import com.galeritos.risk_guard.identity.infrastructure.persistence.repository.UserCredentialRepository;

@ExtendWith(MockitoExtension.class)
class LoginUseCaseTest {
    @Mock
    private UserCredentialRepository userCredentialRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @Mock
    private JwtService jwtService;

    private LoginUseCase useCase;

    @BeforeEach
    void setUp() {
        useCase = new LoginUseCase(userCredentialRepository, passwordEncoder, jwtService);
    }

    @Test
    void shouldLoginAndGenerateToken() {
        LoginCommand command = new LoginCommand("user@example.com", "StrongPass123");
        User user = new User(null, "User", "user@example.com", "12345678901", Role.USER, UserStatus.PENDING);
        UserCredential credential = new UserCredential(null, user, "hash", true);

        when(userCredentialRepository.findByUserEmail(command.email())).thenReturn(Optional.of(credential));
        when(passwordEncoder.matches(command.password(), credential.getPasswordHash())).thenReturn(true);
        when(jwtService.generateToken(user)).thenReturn("token");

        LoginResult result = useCase.execute(command);
        assertEquals("token", result.accessToken());
        assertEquals(UserStatus.PENDING, result.status());
    }

    @Test
    void shouldFailWithInvalidCredentials() {
        LoginCommand command = new LoginCommand("user@example.com", "WrongPass");
        User user = new User(null, "User", "user@example.com", "12345678901", Role.USER, UserStatus.PENDING);
        UserCredential credential = new UserCredential(null, user, "hash", true);

        when(userCredentialRepository.findByUserEmail(command.email())).thenReturn(Optional.of(credential));
        when(passwordEncoder.matches(command.password(), credential.getPasswordHash())).thenReturn(false);

        assertThrows(InvalidCredentialsException.class, () -> useCase.execute(command));
    }
}
