package com.galeritos.risk_guard.identity.infrastructure.persistence.repository;

import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.galeritos.risk_guard.identity.domain.model.User;
import com.galeritos.risk_guard.identity.domain.model.UserCredential;

public interface UserCredentialRepository extends JpaRepository<UserCredential, UUID> {

    Optional<UserCredential> findByUser(User user);

    @Query("""
            SELECT uc FROM UserCredential uc
            JOIN uc.user u
            WHERE LOWER(u.email) = LOWER(:email)
            """)
    Optional<UserCredential> findByUserEmail(@Param("email") String email);

}
