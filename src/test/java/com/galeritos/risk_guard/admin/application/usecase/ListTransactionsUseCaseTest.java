package com.galeritos.risk_guard.admin.application.usecase;

import static org.junit.jupiter.api.Assertions.assertEquals;
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
import com.galeritos.risk_guard.shared.enums.RiskLevel;

@ExtendWith(MockitoExtension.class)
class ListTransactionsUseCaseTest {

    @Mock
    private TransactionRepository repository;

    private ListTransactionsUseCase useCase;

    @BeforeEach
    void setUp() {
        useCase = new ListTransactionsUseCase(repository);
    }

    private Transaction transaction(TransactionStatus status, RiskLevel riskLevel) {
        return new Transaction(UUID.randomUUID(), UUID.randomUUID(), new BigDecimal("100.00"),
                status, FinancialStatus.RESERVED, riskLevel, LocalDateTime.now());
    }

    @Test
    @SuppressWarnings("unchecked")
    void shouldReturnAllTransactionsWhenNoFiltersProvided() {
        List<Transaction> transactions = List.of(
                transaction(TransactionStatus.APPROVED, RiskLevel.LOW),
                transaction(TransactionStatus.DENIED, RiskLevel.HIGH));
        Page<Transaction> page = new PageImpl<>(transactions);
        when(repository.findAll(any(Specification.class), any(Pageable.class))).thenReturn(page);

        Page<Transaction> result = useCase.execute(null, null, null, null, 0, 20);

        assertEquals(2, result.getTotalElements());
        verify(repository).findAll(any(Specification.class), any(Pageable.class));
    }

    @Test
    @SuppressWarnings("unchecked")
    void shouldFilterByStatus() {
        List<Transaction> transactions = List.of(transaction(TransactionStatus.APPROVED, RiskLevel.LOW));
        Page<Transaction> page = new PageImpl<>(transactions);
        when(repository.findAll(any(Specification.class), any(Pageable.class))).thenReturn(page);

        Page<Transaction> result = useCase.execute(TransactionStatus.APPROVED, null, null, null, 0, 20);

        assertEquals(1, result.getTotalElements());
        assertEquals(TransactionStatus.APPROVED, result.getContent().get(0).getStatus());
    }

    @Test
    @SuppressWarnings("unchecked")
    void shouldFilterByRiskLevel() {
        List<Transaction> transactions = List.of(transaction(TransactionStatus.AWAITING_ANALYST, RiskLevel.HIGH));
        Page<Transaction> page = new PageImpl<>(transactions);
        when(repository.findAll(any(Specification.class), any(Pageable.class))).thenReturn(page);

        Page<Transaction> result = useCase.execute(null, RiskLevel.HIGH, null, null, 0, 20);

        assertEquals(1, result.getTotalElements());
        assertEquals(RiskLevel.HIGH, result.getContent().get(0).getRiskLevel());
    }

    @Test
    @SuppressWarnings("unchecked")
    void shouldFilterByDateRange() {
        LocalDateTime from = LocalDateTime.now().minusDays(7);
        LocalDateTime to = LocalDateTime.now();
        Page<Transaction> page = new PageImpl<>(List.of(transaction(TransactionStatus.APPROVED, RiskLevel.LOW)));
        when(repository.findAll(any(Specification.class), any(Pageable.class))).thenReturn(page);

        Page<Transaction> result = useCase.execute(null, null, from, to, 0, 20);

        assertEquals(1, result.getTotalElements());
    }

    @Test
    @SuppressWarnings("unchecked")
    void shouldReturnEmptyPageWhenNoMatches() {
        when(repository.findAll(any(Specification.class), any(Pageable.class))).thenReturn(Page.empty());

        Page<Transaction> result = useCase.execute(TransactionStatus.DISPUTED, RiskLevel.LOW, null, null, 0, 20);

        assertEquals(0, result.getTotalElements());
    }
}
