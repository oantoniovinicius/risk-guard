package com.galeritos.risk_guard.banking.application.usecase;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.galeritos.risk_guard.banking.domain.model.Account;
import com.galeritos.risk_guard.config.BankingProperties;
import com.galeritos.risk_guard.banking.infrastructure.persistence.repository.AccountRepository;

@ExtendWith(MockitoExtension.class)
class CreateAccountForApprovedUserUseCaseTest {
    @Mock
    private AccountRepository accountRepository;

    private CreateAccountForApprovedUserUseCase useCase;

    @BeforeEach
    void setUp() {
        useCase = new CreateAccountForApprovedUserUseCase(
                accountRepository,
                new BankingProperties(new BigDecimal("1000.00")));
    }

    @Test
    void shouldCreateAccountWhenMissing() {
        UUID userId = UUID.randomUUID();
        when(accountRepository.existsByUserId(userId)).thenReturn(false);

        useCase.execute(userId);

        org.mockito.ArgumentCaptor<Account> accountCaptor = org.mockito.ArgumentCaptor.forClass(Account.class);
        verify(accountRepository).save(accountCaptor.capture());

        Account createdAccount = accountCaptor.getValue();
        org.junit.jupiter.api.Assertions.assertEquals(0, createdAccount.getBalance().compareTo(new BigDecimal("1000.00")));
        org.junit.jupiter.api.Assertions.assertEquals(0, createdAccount.getReservedBalance().compareTo(BigDecimal.ZERO));
    }

    @Test
    void shouldBeIdempotentWhenAccountAlreadyExists() {
        UUID userId = UUID.randomUUID();
        when(accountRepository.existsByUserId(userId)).thenReturn(true);

        useCase.execute(userId);

        verify(accountRepository, never()).save(any());
    }
}
