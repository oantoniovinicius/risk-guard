package com.galeritos.risk_guard.banking.application.usecase;

import java.math.BigDecimal;
import java.util.UUID;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.galeritos.risk_guard.banking.domain.model.Account;
import com.galeritos.risk_guard.banking.infrastructure.persistence.repository.AccountRepository;
import com.galeritos.risk_guard.config.BankingProperties;

@Service
public class CreateAccountForApprovedUserUseCase {
    private final AccountRepository accountRepository;
    private final BankingProperties bankingProperties;

    public CreateAccountForApprovedUserUseCase(AccountRepository accountRepository, BankingProperties bankingProperties) {
        this.accountRepository = accountRepository;
        this.bankingProperties = bankingProperties;
    }

    @Transactional
    public void execute(UUID userId) {
        if (accountRepository.existsByUserId(userId)) {
            return;
        }

        Account account = new Account(
                null,
                userId,
                bankingProperties.initialBalance(),
                BigDecimal.ZERO);
        accountRepository.save(account);
    }
}
