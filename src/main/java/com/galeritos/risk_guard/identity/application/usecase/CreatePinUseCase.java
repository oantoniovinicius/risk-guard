package com.galeritos.risk_guard.identity.application.usecase;

import java.util.Set;
import java.util.UUID;

import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.galeritos.risk_guard.identity.domain.exception.InvalidPinException;
import com.galeritos.risk_guard.identity.domain.exception.PinAlreadyDefinedException;
import com.galeritos.risk_guard.identity.domain.exception.UserNotActiveException;
import com.galeritos.risk_guard.identity.domain.model.UserCredential;
import com.galeritos.risk_guard.identity.domain.model.enums.UserStatus;
import com.galeritos.risk_guard.identity.infrastructure.persistence.repository.UserCredentialRepository;

@Service
public class CreatePinUseCase {

    private static final Set<String> BANNED_PINS = Set.of(
            "0000", "1111", "2222", "3333", "4444", "5555", "6666", "7777", "8888", "9999",
            "0123", "1234", "2345", "3456", "4567", "5678", "6789",
            "9876", "8765", "7654", "6543", "5432", "4321", "3210",
            "1230");

    private final UserCredentialRepository repository;
    private final PasswordEncoder passwordEncoder;

    public CreatePinUseCase(UserCredentialRepository repository, PasswordEncoder passwordEncoder) {
        this.repository = repository;
        this.passwordEncoder = passwordEncoder;
    }

    @Transactional
    public void execute(UUID userId, String rawPin) {
        UserCredential credential = repository.findByUserId(userId)
                .orElseThrow(() -> new IllegalArgumentException("Credential not found"));

        if (credential.getUser().getStatus() != UserStatus.ACTIVE) {
            throw new UserNotActiveException(userId, credential.getUser().getStatus());
        }

        if (credential.getPinHash() != null) {
            throw new PinAlreadyDefinedException();
        }

        validatePinStrength(rawPin);

        credential.setPinHash(passwordEncoder.encode(rawPin));
        repository.save(credential);
    }

    private void validatePinStrength(String pin) {
        if (pin.length() < 4 || !pin.matches("\\d+")) {
            throw new InvalidPinException();
        }

        if (pin.chars().distinct().count() == 1) {
            throw new InvalidPinException();
        }

        if (isSequential(pin)) {
            throw new InvalidPinException();
        }

        if (BANNED_PINS.contains(pin)) {
            throw new InvalidPinException();
        }
    }

    private boolean isSequential(String pin) {
        for (int i = 1; i < pin.length(); i++) {
            int diff = (pin.charAt(i) - '0') - (pin.charAt(i - 1) - '0');
            if (diff != 1 && diff != -1) {
                return false;
            }
        }
        return true;
    }
}