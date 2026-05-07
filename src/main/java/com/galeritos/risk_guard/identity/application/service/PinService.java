package com.galeritos.risk_guard.identity.application.service;

import java.util.UUID;

import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import com.galeritos.risk_guard.identity.domain.exception.InvalidPinException;
import com.galeritos.risk_guard.identity.domain.exception.PinNotConfiguredException;
import com.galeritos.risk_guard.identity.domain.model.UserCredential;
import com.galeritos.risk_guard.identity.infrastructure.persistence.repository.UserCredentialRepository;

@Service
public class PinService {
    private final UserCredentialRepository repository;
    private final PasswordEncoder passwordEncoder;

    public PinService(
            UserCredentialRepository repository,
            PasswordEncoder passwordEncoder) {
        this.repository = repository;
        this.passwordEncoder = passwordEncoder;
    }

    public void validate(UUID userId, String rawPin) {
        UserCredential credential = repository.findByUserId(userId)
                .orElseThrow(() -> new IllegalArgumentException("Credential not found"));

        if (credential.getPinHash() == null) {
            throw new PinNotConfiguredException();
        }

        if (!passwordEncoder.matches(rawPin, credential.getPinHash())) {
            throw new InvalidPinException();
        }
    }
}
