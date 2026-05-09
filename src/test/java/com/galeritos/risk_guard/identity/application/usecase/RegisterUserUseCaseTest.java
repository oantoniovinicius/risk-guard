package com.galeritos.risk_guard.identity.application.usecase;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Optional;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import com.galeritos.risk_guard.identity.application.usecase.dto.RegisterUserCommand;
import com.galeritos.risk_guard.identity.domain.exception.EmailAlreadyInUseException;
import com.galeritos.risk_guard.identity.domain.model.User;
import com.galeritos.risk_guard.identity.domain.model.UserCredential;
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
    void shouldRegisterNewPendingUserWithHashedPassword() {
        RegisterUserCommand command = new RegisterUserCommand(
                "Alice", "alice@example.com", "12345678901", "StrongPass123");

        when(userRepository.findByEmail("alice@example.com")).thenReturn(Optional.empty());
        when(userRepository.existsByDocument(command.document())).thenReturn(false);
        when(passwordEncoder.encode(command.password())).thenReturn("hash");
        when(userRepository.save(any(User.class))).thenAnswer(inv -> inv.getArgument(0));

        User user = useCase.execute(command);

        assertEquals(UserStatus.PENDING, user.getStatus());
        assertEquals(Role.USER, user.getRole());
        verify(userCredentialRepository).save(any());
    }

    @Test
    void shouldResubmitRejectedUserAndResetCredential() {
        UUID userId = UUID.randomUUID();
        RegisterUserCommand command = new RegisterUserCommand(
                "Alice Updated", "alice@example.com", "12345678901", "NewPass456");

        User rejected = new User(userId, "Alice", "alice@example.com", "12345678901", Role.USER, UserStatus.REJECTED);
        UserCredential credential = new UserCredential(UUID.randomUUID(), rejected, "oldhash", true, "pinhash");

        when(userRepository.findByEmail("alice@example.com")).thenReturn(Optional.of(rejected));
        when(userRepository.existsByDocumentAndIdNot("12345678901", userId)).thenReturn(false);
        when(userRepository.save(rejected)).thenReturn(rejected);
        when(userCredentialRepository.findByUserId(userId)).thenReturn(Optional.of(credential));
        when(passwordEncoder.encode("NewPass456")).thenReturn("newhash");

        User result = useCase.execute(command);

        assertEquals(UserStatus.PENDING, result.getStatus());
        assertEquals("Alice Updated", result.getName());
        verify(userCredentialRepository).save(credential);
    }

    @Test
    void shouldThrowWhenEmailBelongsToActiveUser() {
        RegisterUserCommand command = new RegisterUserCommand(
                "Alice", "alice@example.com", "12345678901", "StrongPass123");

        User activeUser = new User(UUID.randomUUID(), "Alice", "alice@example.com", "12345678901", Role.USER, UserStatus.ACTIVE);
        when(userRepository.findByEmail("alice@example.com")).thenReturn(Optional.of(activeUser));

        assertThrows(EmailAlreadyInUseException.class, () -> useCase.execute(command));
        verify(userRepository, never()).save(any());
    }

    @Test
    void shouldThrowWhenEmailBelongsToBlockedUser() {
        RegisterUserCommand command = new RegisterUserCommand(
                "Alice", "alice@example.com", "12345678901", "StrongPass123");

        User blockedUser = new User(UUID.randomUUID(), "Alice", "alice@example.com", "12345678901", Role.USER, UserStatus.ACTIVE);
        blockedUser.block();
        when(userRepository.findByEmail("alice@example.com")).thenReturn(Optional.of(blockedUser));

        assertThrows(EmailAlreadyInUseException.class, () -> useCase.execute(command));
        verify(userRepository, never()).save(any());
    }
}
