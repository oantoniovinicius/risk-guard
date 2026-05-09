package com.galeritos.risk_guard.identity.domain.model;

import java.time.LocalDateTime;
import java.util.UUID;

import com.galeritos.risk_guard.identity.domain.model.enums.Role;
import com.galeritos.risk_guard.identity.domain.model.enums.UserStatus;
import com.galeritos.risk_guard.identity.domain.exception.InvalidUserStatusTransitionException;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Getter;

@Entity
@Getter
@Table(name = "users")
public class User {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(nullable = false)
    private String name;

    @Column(nullable = false, unique = true)
    private String email;

    @Column(nullable = false, unique = true)
    private String document;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private Role role;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private UserStatus status;

    @Column(name = "suspect", nullable = false)
    private boolean suspect;

    @Column(nullable = false)
    private LocalDateTime createdAt;

    @Column(name = "rejection_count", nullable = false)
    private int rejectionCount;

    protected User() {
    }

    public User(UUID id, String name, String email, String document, Role role, UserStatus status) {
        this.id = id;
        this.name = name;
        this.email = email;
        this.document = document;
        this.role = role;
        this.status = status;
        this.suspect = false;
        this.createdAt = LocalDateTime.now();
        this.rejectionCount = 0;
    }

    public void markAsSuspect() {
        this.suspect = true;
    }

    public boolean isSuspect() {
        return this.suspect;
    }

    public void activate() {
        if (status != UserStatus.PENDING) {
            throw new InvalidUserStatusTransitionException(status, UserStatus.ACTIVE);
        }
        this.status = UserStatus.ACTIVE;
    }

    public void reject() {
        if (status != UserStatus.PENDING) {
            throw new InvalidUserStatusTransitionException(status, UserStatus.REJECTED);
        }
        this.rejectionCount++;
        this.status = UserStatus.REJECTED;
    }

    public void rejectAndBlock() {
        if (status != UserStatus.PENDING) {
            throw new InvalidUserStatusTransitionException(status, UserStatus.BLOCKED);
        }
        this.rejectionCount++;
        this.status = UserStatus.BLOCKED;
    }

    public void resubmit(String name, String document) {
        if (status != UserStatus.REJECTED) {
            throw new InvalidUserStatusTransitionException(status, UserStatus.PENDING);
        }
        this.name = name;
        this.document = document;
        this.status = UserStatus.PENDING;
    }

    public void suspend() {
        if (status != UserStatus.ACTIVE) {
            throw new InvalidUserStatusTransitionException(status, UserStatus.SUSPENDED);
        }
        this.status = UserStatus.SUSPENDED;
    }

    public void unsuspend() {
        if (status != UserStatus.SUSPENDED) {
            throw new InvalidUserStatusTransitionException(status, UserStatus.ACTIVE);
        }
        this.status = UserStatus.ACTIVE;
    }

    public void block() {
        if (status != UserStatus.ACTIVE) {
            throw new InvalidUserStatusTransitionException(status, UserStatus.BLOCKED);
        }
        this.status = UserStatus.BLOCKED;
    }

    public void unblock() {
        if (status != UserStatus.BLOCKED) {
            throw new InvalidUserStatusTransitionException(status, UserStatus.ACTIVE);
        }
        this.status = UserStatus.ACTIVE;
    }

    public void changeRole(Role newRole) {
        this.role = newRole;
    }
}
