package com.galeritos.risk_guard.identity.application.service;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
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
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.util.ReflectionTestUtils;

import com.galeritos.risk_guard.identity.domain.exception.InvalidPinException;
import com.galeritos.risk_guard.identity.domain.exception.PinBlockedException;
import com.galeritos.risk_guard.identity.domain.exception.PinNotConfiguredException;
import com.galeritos.risk_guard.identity.domain.model.User;
import com.galeritos.risk_guard.identity.domain.model.UserCredential;
import com.galeritos.risk_guard.identity.domain.model.enums.Role;
import com.galeritos.risk_guard.identity.domain.model.enums.UserStatus;
import com.galeritos.risk_guard.identity.infrastructure.persistence.repository.UserCredentialRepository;

@ExtendWith(MockitoExtension.class)
class PinServiceTest {

    @Mock
    private UserCredentialRepository repository;

    @Mock
    private PasswordEncoder passwordEncoder;

    private PinService pinService;

    @BeforeEach
    void setUp() {
        pinService = new PinService(repository, passwordEncoder);
    }

    private UserCredential credential(String pinHash, int failedAttempts) {
        User user = new User(UUID.randomUUID(), "Alice", "alice@example.com", "12345678901", Role.USER, UserStatus.ACTIVE);
        UserCredential cred = new UserCredential(UUID.randomUUID(), user, "hashedpw", true, pinHash);
        ReflectionTestUtils.setField(cred, "failedPinAttempts", failedAttempts);
        return cred;
    }

    @Test
    void shouldPassAndResetCounterWhenPinIsCorrect() {
        UUID userId = UUID.randomUUID();
        UserCredential cred = credential("hashed_pin", 3);

        when(repository.findByUserId(userId)).thenReturn(Optional.of(cred));
        when(passwordEncoder.matches("4592", "hashed_pin")).thenReturn(true);

        assertDoesNotThrow(() -> pinService.validate(userId, "4592"));

        ArgumentCaptor<UserCredential> captor = ArgumentCaptor.forClass(UserCredential.class);
        verify(repository).save(captor.capture());
        assertEquals(0, captor.getValue().getFailedPinAttempts());
    }

    @Test
    void shouldIncrementCounterAndThrowWhenPinIsWrong() {
        UUID userId = UUID.randomUUID();
        UserCredential cred = credential("hashed_pin", 2);

        when(repository.findByUserId(userId)).thenReturn(Optional.of(cred));
        when(passwordEncoder.matches("wrong", "hashed_pin")).thenReturn(false);

        assertThrows(InvalidPinException.class, () -> pinService.validate(userId, "wrong"));

        ArgumentCaptor<UserCredential> captor = ArgumentCaptor.forClass(UserCredential.class);
        verify(repository).save(captor.capture());
        assertEquals(3, captor.getValue().getFailedPinAttempts());
    }

    @Test
    void shouldThrowPinBlockedWhenMaxAttemptsReached() {
        UUID userId = UUID.randomUUID();
        UserCredential cred = credential("hashed_pin", PinService.MAX_PIN_ATTEMPTS);

        when(repository.findByUserId(userId)).thenReturn(Optional.of(cred));

        assertThrows(PinBlockedException.class, () -> pinService.validate(userId, "4592"));
        verify(repository, never()).save(any());
    }

    @Test
    void shouldThrowPinBlockedEvenWithCorrectPinWhenBlocked() {
        UUID userId = UUID.randomUUID();
        UserCredential cred = credential("hashed_pin", PinService.MAX_PIN_ATTEMPTS);

        when(repository.findByUserId(userId)).thenReturn(Optional.of(cred));

        assertThrows(PinBlockedException.class, () -> pinService.validate(userId, "4592"));
        verify(passwordEncoder, never()).matches(any(), any());
    }

    @Test
    void shouldThrowPinNotConfiguredWhenPinHashIsNull() {
        UUID userId = UUID.randomUUID();
        UserCredential cred = credential(null, 0);

        when(repository.findByUserId(userId)).thenReturn(Optional.of(cred));

        PinNotConfiguredException ex = assertThrows(PinNotConfiguredException.class,
                () -> pinService.validate(userId, "4592"));
        assertEquals("User must define PIN before performing this action.", ex.getMessage());
        verify(repository, never()).save(any());
    }
}
