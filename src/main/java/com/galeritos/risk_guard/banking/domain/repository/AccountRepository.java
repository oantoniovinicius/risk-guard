package com.galeritos.risk_guard.banking.domain.repository;

import java.util.Optional;
import java.util.UUID;

import com.galeritos.risk_guard.banking.domain.model.Account;

public interface AccountRepository {

    Optional<Account> findByUserId(UUID userId);

    Optional<Account> findByUserIdForUpdate(UUID userId);

    boolean existsByUserId(UUID userId);

    Account save(Account account);

}