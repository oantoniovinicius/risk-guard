package com.galeritos.risk_guard.identity.application.usecase;

import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import com.galeritos.risk_guard.identity.application.security.JwtService;
import com.galeritos.risk_guard.identity.application.usecase.dto.LoginCommand;
import com.galeritos.risk_guard.identity.application.usecase.dto.LoginResult;
import com.galeritos.risk_guard.identity.domain.exception.InvalidCredentialsException;
import com.galeritos.risk_guard.identity.domain.exception.UserCredentialDisabledException;
import com.galeritos.risk_guard.identity.domain.model.User;
import com.galeritos.risk_guard.identity.domain.model.UserCredential;
import com.galeritos.risk_guard.identity.infrastructure.persistence.repository.UserCredentialRepository;

@Service
public class LoginUseCase {
    private final UserCredentialRepository userCredentialRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;

    public LoginUseCase(
            UserCredentialRepository userCredentialRepository,
            PasswordEncoder passwordEncoder,
            JwtService jwtService) {
        this.userCredentialRepository = userCredentialRepository;
        this.passwordEncoder = passwordEncoder;
        this.jwtService = jwtService;
    }

    public LoginResult execute(LoginCommand command) {
        String normalizedEmail = command.email().trim().toLowerCase();
        UserCredential credential = userCredentialRepository.findByUserEmail(normalizedEmail)
                .orElseThrow(InvalidCredentialsException::new);

        if (!credential.isEnabled()) {
            throw new UserCredentialDisabledException();
        }

        if (!passwordEncoder.matches(command.password(), credential.getPasswordHash())) {
            throw new InvalidCredentialsException();
        }

        User user = credential.getUser();
        String token = jwtService.generateToken(user);

        return new LoginResult(
                token,
                user.getId(),
                user.getEmail(),
                user.getRole(),
                user.getStatus());
    }
}
