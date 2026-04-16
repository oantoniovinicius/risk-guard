package com.galeritos.risk_guard.identity.application.security;

import java.time.Instant;
import java.util.Date;
import java.util.Map;
import java.util.UUID;

import javax.crypto.SecretKey;

import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.stereotype.Service;

import com.galeritos.risk_guard.config.JwtProperties;
import com.galeritos.risk_guard.identity.domain.model.User;
import com.galeritos.risk_guard.identity.domain.model.enums.Role;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;

@Service
public class JwtService {
    private final JwtProperties jwtProperties;
    private final SecretKey signingKey;

    public JwtService(JwtProperties jwtProperties) {
        this.jwtProperties = jwtProperties;
        this.signingKey = Keys.hmacShaKeyFor(Decoders.BASE64.decode(jwtProperties.secretBase64()));
    }

    public String generateToken(User user) {
        Instant now = Instant.now();
        Instant expiresAt = now.plusMillis(jwtProperties.expirationMs());

        return Jwts.builder()
                .issuer(jwtProperties.issuer())
                .subject(user.getEmail())
                .id(UUID.randomUUID().toString())
                .issuedAt(Date.from(now))
                .expiration(Date.from(expiresAt))
                .claims(Map.of(
                        "userId", user.getId().toString(),
                        "role", user.getRole().name()))
                .signWith(signingKey)
                .compact();
    }

    public AuthenticatedUser parseAuthenticatedUser(String token) {
        Claims claims = extractClaims(token);

        UUID userId = UUID.fromString(claims.get("userId", String.class));
        Role role = Role.valueOf(claims.get("role", String.class));
        String email = claims.getSubject();

        return new AuthenticatedUser(
                userId,
                email,
                role,
                java.util.List.of(new SimpleGrantedAuthority("ROLE_" + role.name())));
    }

    public boolean isTokenValid(String token) {
        try {
            extractClaims(token);
            return true;
        } catch (JwtException | IllegalArgumentException ex) {
            return false;
        }
    }

    private Claims extractClaims(String token) {
        return Jwts.parser()
                .verifyWith(signingKey)
                .requireIssuer(jwtProperties.issuer())
                .build()
                .parseSignedClaims(token)
                .getPayload();
    }
}
