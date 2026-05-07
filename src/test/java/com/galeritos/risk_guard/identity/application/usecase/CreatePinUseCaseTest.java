package com.galeritos.risk_guard.identity.application.usecase;

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
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import com.galeritos.risk_guard.identity.domain.exception.InvalidPinException;
import com.galeritos.risk_guard.identity.domain.exception.PinAlreadyDefinedException;
import com.galeritos.risk_guard.identity.domain.exception.UserNotActiveException;
import com.galeritos.risk_guard.identity.domain.model.User;
import com.galeritos.risk_guard.identity.domain.model.UserCredential;
import com.galeritos.risk_guard.identity.domain.model.enums.Role;
import com.galeritos.risk_guard.identity.domain.model.enums.UserStatus;
import com.galeritos.risk_guard.identity.infrastructure.persistence.repository.UserCredentialRepository;

@ExtendWith(MockitoExtension.class)
class CreatePinUseCaseTest {

    @Mock
    private UserCredentialRepository repository;

    @Mock
    private PasswordEncoder passwordEncoder;

    private CreatePinUseCase useCase;

    @BeforeEach
    void setUp() {
        useCase = new CreatePinUseCase(repository, passwordEncoder);
    }

    @Test
    void shouldCreatePinForActiveUser() {
        UUID userId = UUID.randomUUID();
        User user = new User(userId, "Alice", "alice@example.com", "12345678901", Role.USER, UserStatus.ACTIVE);
        UserCredential credential = new UserCredential(UUID.randomUUID(), user, "hashedpw", true, null);

        when(repository.findByUserId(userId)).thenReturn(Optional.of(credential));
        when(passwordEncoder.encode("4592")).thenReturn("hashed_pin");

        useCase.execute(userId, "4592");

        verify(repository).save(credential);
    }

    @Test
    void shouldThrowWhenUserIsNotActive() {
        UUID userId = UUID.randomUUID();
        User user = new User(userId, "Bob", "bob@example.com", "12345678902", Role.USER, UserStatus.PENDING);
        UserCredential credential = new UserCredential(UUID.randomUUID(), user, "hashedpw", true, null);

        when(repository.findByUserId(userId)).thenReturn(Optional.of(credential));

        assertThrows(UserNotActiveException.class, () -> useCase.execute(userId, "4592"));
        verify(repository, never()).save(any());
    }

    @Test
    void shouldThrowWhenPinAlreadyDefined() {
        UUID userId = UUID.randomUUID();
        User user = new User(userId, "Carol", "carol@example.com", "12345678903", Role.USER, UserStatus.ACTIVE);
        UserCredential credential = new UserCredential(UUID.randomUUID(), user, "hashedpw", true, "existing_hash");

        when(repository.findByUserId(userId)).thenReturn(Optional.of(credential));

        assertThrows(PinAlreadyDefinedException.class, () -> useCase.execute(userId, "4592"));
        verify(repository, never()).save(any());
    }

    @ParameterizedTest
    @ValueSource(strings = { "123", "abc", "1111", "11111", "1234", "0000", "4321", "9876" })
    void shouldThrowForWeakPins(String pin) {
        UUID userId = UUID.randomUUID();
        User user = new User(userId, "Dan", "dan@example.com", "12345678904", Role.USER, UserStatus.ACTIVE);
        UserCredential credential = new UserCredential(UUID.randomUUID(), user, "hashedpw", true, null);

        when(repository.findByUserId(userId)).thenReturn(Optional.of(credential));

        assertThrows(InvalidPinException.class, () -> useCase.execute(userId, pin));
        verify(repository, never()).save(any());
    }

    @ParameterizedTest
    @ValueSource(strings = { "4592", "8274", "1379", "29384" })
    void shouldAcceptStrongPins(String pin) {
        UUID userId = UUID.randomUUID();
        User user = new User(userId, "Eve", "eve@example.com", "12345678905", Role.USER, UserStatus.ACTIVE);
        UserCredential credential = new UserCredential(UUID.randomUUID(), user, "hashedpw", true, null);

        when(repository.findByUserId(userId)).thenReturn(Optional.of(credential));
        when(passwordEncoder.encode(pin)).thenReturn("hashed");

        useCase.execute(userId, pin);

        verify(repository).save(credential);
    }
}
