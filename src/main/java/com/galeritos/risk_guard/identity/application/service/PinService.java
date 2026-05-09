package com.galeritos.risk_guard.identity.application.service;

import java.util.UUID;

import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import com.galeritos.risk_guard.identity.domain.exception.InvalidPinException;
import com.galeritos.risk_guard.identity.domain.exception.PinBlockedException;
import com.galeritos.risk_guard.identity.domain.exception.PinNotConfiguredException;
import com.galeritos.risk_guard.identity.domain.model.UserCredential;
import com.galeritos.risk_guard.identity.infrastructure.persistence.repository.UserCredentialRepository;

@Service
public class PinService {

    static final int MAX_PIN_ATTEMPTS = 5;

    private final UserCredentialRepository repository;
    private final PasswordEncoder passwordEncoder;

    public PinService(
            UserCredentialRepository repository,
            PasswordEncoder passwordEncoder) {
        this.repository = repository;
        this.passwordEncoder = passwordEncoder;
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW, noRollbackFor = InvalidPinException.class)
    public void validate(UUID userId, String rawPin) {
        UserCredential credential = repository.findByUserId(userId)
                .orElseThrow(() -> new IllegalArgumentException("Credential not found"));

        if (credential.getPinHash() == null) {
            throw new PinNotConfiguredException();
        }

        if (credential.getFailedPinAttempts() >= MAX_PIN_ATTEMPTS) {
            throw new PinBlockedException();
        }

        if (!passwordEncoder.matches(rawPin, credential.getPinHash())) {
            credential.recordFailedAttempt();
            repository.save(credential);
            throw new InvalidPinException();
        }

        credential.resetFailedAttempts();
        repository.save(credential);
    }
}
