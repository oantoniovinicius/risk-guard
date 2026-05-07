package com.galeritos.risk_guard.identity.application.service;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.when;

import java.util.Optional;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import com.galeritos.risk_guard.identity.domain.exception.InvalidPinException;
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

    @Test
    void shouldPassWhenPinIsCorrect() {
        UUID userId = UUID.randomUUID();
        User user = new User(userId, "Alice", "alice@example.com", "12345678901", Role.USER, UserStatus.ACTIVE);
        UserCredential credential = new UserCredential(UUID.randomUUID(), user, "hashedpw", true, "hashed_pin");

        when(repository.findByUserId(userId)).thenReturn(Optional.of(credential));
        when(passwordEncoder.matches("4592", "hashed_pin")).thenReturn(true);

        assertDoesNotThrow(() -> pinService.validate(userId, "4592"));
    }

    @Test
    void shouldThrowWhenPinIsWrong() {
        UUID userId = UUID.randomUUID();
        User user = new User(userId, "Bob", "bob@example.com", "12345678902", Role.USER, UserStatus.ACTIVE);
        UserCredential credential = new UserCredential(UUID.randomUUID(), user, "hashedpw", true, "hashed_pin");

        when(repository.findByUserId(userId)).thenReturn(Optional.of(credential));
        when(passwordEncoder.matches("wrong", "hashed_pin")).thenReturn(false);

        assertThrows(InvalidPinException.class, () -> pinService.validate(userId, "wrong"));
    }

    @Test
    void shouldThrowPinNotConfiguredWhenPinHashIsNull() {
        UUID userId = UUID.randomUUID();
        User user = new User(userId, "Carol", "carol@example.com", "12345678903", Role.USER, UserStatus.ACTIVE);
        UserCredential credential = new UserCredential(UUID.randomUUID(), user, "hashedpw", true, null);

        when(repository.findByUserId(userId)).thenReturn(Optional.of(credential));

        PinNotConfiguredException ex = assertThrows(PinNotConfiguredException.class,
                () -> pinService.validate(userId, "4592"));
        org.junit.jupiter.api.Assertions.assertEquals(
                "User must define PIN before performing this action.", ex.getMessage());
    }
}
