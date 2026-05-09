package com.galeritos.risk_guard.identity.application.usecase;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Optional;
import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import com.galeritos.risk_guard.identity.domain.exception.UserNotFoundException;
import com.galeritos.risk_guard.identity.domain.model.UserCredential;
import com.galeritos.risk_guard.identity.infrastructure.persistence.repository.UserCredentialRepository;
import com.galeritos.risk_guard.identity.infrastructure.persistence.repository.UserRepository;

@ExtendWith(MockitoExtension.class)
class ResetPinLockUseCaseTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private UserCredentialRepository userCredentialRepository;

    @InjectMocks
    private ResetPinLockUseCase useCase;

    private UserCredential buildCredential(UUID userId) {
        UserCredential credential = new UserCredential(UUID.randomUUID(), null, "hash", true, null);
        ReflectionTestUtils.setField(credential, "failedPinAttempts", 3);
        return credential;
    }

    @Test
    void shouldResetFailedAttemptsAndSave() {
        UUID userId = UUID.randomUUID();
        UserCredential credential = buildCredential(userId);

        when(userRepository.existsById(userId)).thenReturn(true);
        when(userCredentialRepository.findByUserId(userId)).thenReturn(Optional.of(credential));

        useCase.execute(userId);

        verify(userCredentialRepository).save(credential);
    }

    @Test
    void shouldThrowWhenUserNotFound() {
        UUID userId = UUID.randomUUID();
        when(userRepository.existsById(userId)).thenReturn(false);

        assertThrows(UserNotFoundException.class, () -> useCase.execute(userId));

        verify(userCredentialRepository, never()).findByUserId(any());
        verify(userCredentialRepository, never()).save(any());
    }
}
