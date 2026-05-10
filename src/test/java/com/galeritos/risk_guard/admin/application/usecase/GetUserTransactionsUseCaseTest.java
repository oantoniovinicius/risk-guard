package com.galeritos.risk_guard.admin.application.usecase;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;

import com.galeritos.risk_guard.banking.domain.model.Transaction;
import com.galeritos.risk_guard.banking.domain.model.enums.FinancialStatus;
import com.galeritos.risk_guard.banking.domain.model.enums.TransactionStatus;
import com.galeritos.risk_guard.banking.infrastructure.persistence.repository.TransactionRepository;
import com.galeritos.risk_guard.identity.domain.exception.UserNotFoundException;
import com.galeritos.risk_guard.identity.infrastructure.persistence.repository.UserRepository;

@ExtendWith(MockitoExtension.class)
class GetUserTransactionsUseCaseTest {

    @Mock
    private TransactionRepository transactionRepository;

    @Mock
    private UserRepository userRepository;

    private GetUserTransactionsUseCase useCase;

    @BeforeEach
    void setUp() {
        useCase = new GetUserTransactionsUseCase(transactionRepository, userRepository);
    }

    @Test
    void shouldReturnPagedTransactionsForExistingUser() {
        UUID userId = UUID.randomUUID();
        List<Transaction> transactions = List.of(
                new Transaction(userId, UUID.randomUUID(), new BigDecimal("100.00"),
                        TransactionStatus.APPROVED, FinancialStatus.SETTLED, null, LocalDateTime.now()),
                new Transaction(UUID.randomUUID(), userId, new BigDecimal("50.00"),
                        TransactionStatus.DENIED, FinancialStatus.REVERTED, null, LocalDateTime.now()));
        Page<Transaction> page = new PageImpl<>(transactions);

        when(userRepository.existsById(userId)).thenReturn(true);
        when(transactionRepository.findAll(any(Specification.class), any(Pageable.class))).thenReturn(page);

        Page<Transaction> result = useCase.execute(userId, null, null, null, null, 0, 20);

        assertEquals(2, result.getTotalElements());
        verify(transactionRepository).findAll(any(Specification.class), any(Pageable.class));
    }

    @Test
    void shouldReturnEmptyPageWhenUserHasNoTransactions() {
        UUID userId = UUID.randomUUID();
        when(userRepository.existsById(userId)).thenReturn(true);
        when(transactionRepository.findAll(any(Specification.class), any(Pageable.class)))
                .thenReturn(Page.empty());

        Page<Transaction> result = useCase.execute(userId, null, null, null, null, 0, 20);

        assertEquals(0, result.getTotalElements());
    }

    @Test
    void shouldThrowWhenUserNotFound() {
        UUID userId = UUID.randomUUID();
        when(userRepository.existsById(userId)).thenReturn(false);

        assertThrows(UserNotFoundException.class, () -> useCase.execute(userId, null, null, null, null, 0, 20));
    }
}
