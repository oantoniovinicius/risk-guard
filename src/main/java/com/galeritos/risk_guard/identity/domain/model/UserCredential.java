package com.galeritos.risk_guard.identity.domain.model;

import java.util.UUID;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;
import lombok.Getter;

@Entity
@Getter
@Table(name = "user_credentials")
public class UserCredential {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @OneToOne
    @JoinColumn(name = "user_id", nullable = false, unique = true)
    private User user;

    @Column(nullable = false)
    private String passwordHash;

    @Column(nullable = false)
    private boolean enabled;

    @Column(name = "pin_hash")
    private String pinHash;

    @Column(name = "failed_pin_attempts", nullable = false)
    private int failedPinAttempts;

    protected UserCredential() {
    }

    public UserCredential(UUID id, User user, String passwordHash, boolean enabled, String pinHash) {
        this.id = id;
        this.user = user;
        this.passwordHash = passwordHash;
        this.enabled = enabled;
        this.pinHash = pinHash;
        this.failedPinAttempts = 0;
    }

    public void setPinHash(String pinHash) {
        this.pinHash = pinHash;
    }

    public void recordFailedAttempt() {
        this.failedPinAttempts++;
    }

    public void resetFailedAttempts() {
        this.failedPinAttempts = 0;
    }

    public void clearPin() {
        this.pinHash = null;
        this.failedPinAttempts = 0;
    }

    public void resetForResubmission(String newPasswordHash) {
        this.passwordHash = newPasswordHash;
        this.pinHash = null;
        this.failedPinAttempts = 0;
    }
}
