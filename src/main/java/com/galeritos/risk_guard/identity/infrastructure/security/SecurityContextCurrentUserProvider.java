package com.galeritos.risk_guard.identity.infrastructure.security;

import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;

import com.galeritos.risk_guard.identity.application.security.AuthenticatedUser;
import com.galeritos.risk_guard.identity.application.security.CurrentUserProvider;
import com.galeritos.risk_guard.identity.domain.exception.UnauthenticatedAccessException;

@Component
public class SecurityContextCurrentUserProvider implements CurrentUserProvider {
    @Override
    public AuthenticatedUser getAuthenticatedUser() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();

        if (authentication == null || !authentication.isAuthenticated()) {
            throw new UnauthenticatedAccessException();
        }

        Object principal = authentication.getPrincipal();
        if (!(principal instanceof AuthenticatedUser user)) {
            throw new UnauthenticatedAccessException();
        }

        return user;
    }
}
