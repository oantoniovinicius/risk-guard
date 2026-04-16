package com.galeritos.risk_guard.identity.application.security;

public interface CurrentUserProvider {
    AuthenticatedUser getAuthenticatedUser();
}
