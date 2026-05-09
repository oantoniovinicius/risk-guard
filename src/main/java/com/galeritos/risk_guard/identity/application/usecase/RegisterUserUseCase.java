package com.galeritos.risk_guard.identity.application.usecase;

import java.util.Optional;

import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.galeritos.risk_guard.identity.application.usecase.dto.RegisterUserCommand;
import com.galeritos.risk_guard.identity.domain.exception.DocumentAlreadyInUseException;
import com.galeritos.risk_guard.identity.domain.exception.EmailAlreadyInUseException;
import com.galeritos.risk_guard.identity.domain.model.User;
import com.galeritos.risk_guard.identity.domain.model.UserCredential;
import com.galeritos.risk_guard.identity.domain.model.enums.Role;
import com.galeritos.risk_guard.identity.domain.model.enums.UserStatus;
import com.galeritos.risk_guard.identity.infrastructure.persistence.repository.UserCredentialRepository;
import com.galeritos.risk_guard.identity.infrastructure.persistence.repository.UserRepository;

@Service
public class RegisterUserUseCase {
    private final UserRepository userRepository;
    private final UserCredentialRepository userCredentialRepository;
    private final PasswordEncoder passwordEncoder;

    public RegisterUserUseCase(
            UserRepository userRepository,
            UserCredentialRepository userCredentialRepository,
            PasswordEncoder passwordEncoder) {
        this.userRepository = userRepository;
        this.userCredentialRepository = userCredentialRepository;
        this.passwordEncoder = passwordEncoder;
    }

    @Transactional
    public User execute(RegisterUserCommand command) {
        String normalizedEmail = command.email().trim().toLowerCase();
        String normalizedDocument = command.document().trim();

        Optional<User> existingByEmail = userRepository.findByEmail(normalizedEmail);

        if (existingByEmail.isPresent()) {
            User existing = existingByEmail.get();
            if (existing.getStatus() != UserStatus.REJECTED) {
                throw new EmailAlreadyInUseException(normalizedEmail);
            }
            if (userRepository.existsByDocumentAndIdNot(normalizedDocument, existing.getId())) {
                throw new DocumentAlreadyInUseException(normalizedDocument);
            }
            existing.resubmit(command.name().trim(), normalizedDocument);
            User saved = userRepository.save(existing);

            UserCredential credential = userCredentialRepository.findByUserId(saved.getId())
                    .orElseThrow(() -> new IllegalStateException("Credential missing for user " + saved.getId()));
            credential.resetForResubmission(passwordEncoder.encode(command.password()));
            userCredentialRepository.save(credential);

            return saved;
        }

        if (userRepository.existsByDocument(normalizedDocument)) {
            throw new DocumentAlreadyInUseException(normalizedDocument);
        }

        User user = new User(
                null,
                command.name().trim(),
                normalizedEmail,
                normalizedDocument,
                Role.USER,
                UserStatus.PENDING);
        user = userRepository.save(user);

        UserCredential credential = new UserCredential(
                null,
                user,
                passwordEncoder.encode(command.password()),
                true,
                null);
        userCredentialRepository.save(credential);

        return user;
    }
}
