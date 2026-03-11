package com.galeritos.risk_guard.banking.infrastructure.persistence;

import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import com.galeritos.risk_guard.banking.domain.model.Account;
import com.galeritos.risk_guard.banking.domain.repository.AccountRepository;

import jakarta.persistence.LockModeType;

@Repository
public interface JpaAccountRepository
        extends JpaRepository<Account, UUID>, AccountRepository {

    Optional<Account> findByUserId(UUID userId);

    boolean existsByUserId(UUID userId);

    @Override
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("""
                SELECT a FROM Account a
                WHERE a.userId = :userId
            """)
    Optional<Account> findByUserIdForUpdate(UUID userId);
}
