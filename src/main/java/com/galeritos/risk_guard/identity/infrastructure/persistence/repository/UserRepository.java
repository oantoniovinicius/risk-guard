package com.galeritos.risk_guard.identity.infrastructure.persistence.repository;

import java.util.Optional;
import java.util.UUID;
import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;

import com.galeritos.risk_guard.identity.domain.model.User;
import com.galeritos.risk_guard.identity.domain.model.enums.UserStatus;

import jakarta.persistence.LockModeType;

public interface UserRepository extends JpaRepository<User, UUID> {

    Optional<User> findByEmail(String email);

    Optional<User> findByDocument(String document);

    boolean existsByEmail(String email);

    boolean existsByDocument(String document);

    List<User> findAllByStatusOrderByCreatedAtAsc(UserStatus status);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("""
                SELECT u FROM User u
                WHERE u.id = :userId
            """)
    Optional<User> findByIdForUpdate(UUID userId);

}
