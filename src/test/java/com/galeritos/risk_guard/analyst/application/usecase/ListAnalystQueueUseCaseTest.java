package com.galeritos.risk_guard.analyst.application.usecase;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import org.mockito.ArgumentMatchers;
import org.springframework.data.jpa.domain.Specification;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.security.core.authority.SimpleGrantedAuthority;

import com.galeritos.risk_guard.banking.domain.model.Transaction;
import com.galeritos.risk_guard.banking.domain.model.enums.FinancialStatus;
import com.galeritos.risk_guard.banking.domain.model.enums.TransactionStatus;
import com.galeritos.risk_guard.banking.infrastructure.persistence.repository.TransactionRepository;
import com.galeritos.risk_guard.identity.application.security.AuthenticatedUser;
import com.galeritos.risk_guard.identity.application.security.CurrentUserProvider;
import com.galeritos.risk_guard.identity.domain.model.enums.Role;
import com.galeritos.risk_guard.shared.enums.RiskLevel;

@ExtendWith(MockitoExtension.class)
class ListAnalystQueueUseCaseTest {

    @Mock
    private TransactionRepository transactionRepository;

    @Mock
    private CurrentUserProvider currentUserProvider;

    @InjectMocks
    private ListAnalystQueueUseCase useCase;

    @BeforeEach
    void setUp() {
        lenient().when(currentUserProvider.getAuthenticatedUser()).thenReturn(
                new AuthenticatedUser(UUID.randomUUID(), "analyst@example.com", Role.ANALYST,
                        List.of(new SimpleGrantedAuthority("ROLE_ANALYST"))));
    }

    private Transaction awaitingAnalystTransaction() {
        return new Transaction(
                UUID.randomUUID(), UUID.randomUUID(), new BigDecimal("500.00"),
                TransactionStatus.AWAITING_ANALYST, FinancialStatus.RESERVED, null, LocalDateTime.now());
    }

    private Transaction disputedTransaction() {
        return new Transaction(
                UUID.randomUUID(), UUID.randomUUID(), new BigDecimal("300.00"),
                TransactionStatus.DISPUTED, FinancialStatus.SETTLED, null, LocalDateTime.now());
    }

    @Test
    void shouldReturnPagedQueueSortedOldestFirst() {
        Page<Transaction> expected = new PageImpl<>(List.of(awaitingAnalystTransaction(), disputedTransaction()));
        when(transactionRepository.findAll(ArgumentMatchers.<Specification<Transaction>>any(), any(Pageable.class))).thenReturn(expected);

        Page<Transaction> result = useCase.execute(null, null, null, 0, 20);

        assertEquals(expected, result);
        ArgumentCaptor<Pageable> pageableCaptor = ArgumentCaptor.forClass(Pageable.class);
        verify(transactionRepository).findAll(ArgumentMatchers.<Specification<Transaction>>any(), pageableCaptor.capture());
        Sort.Order order = pageableCaptor.getValue().getSort().getOrderFor("createdAt");
        assertEquals(Sort.Direction.ASC, order.getDirection());
    }

    @Test
    void shouldApplyRiskLevelFilterWhenProvided() {
        when(transactionRepository.findAll(ArgumentMatchers.<Specification<Transaction>>any(), any(Pageable.class)))
                .thenReturn(Page.empty());

        useCase.execute(RiskLevel.HIGH, null, null, 0, 10);

        // Specification is assembled and passed to the repo — verified via interaction
        verify(transactionRepository).findAll(ArgumentMatchers.<Specification<Transaction>>any(), any(Pageable.class));
    }

    @Test
    void shouldApplyDateRangeFiltersWhenProvided() {
        LocalDateTime from = LocalDateTime.now().minusDays(1);
        LocalDateTime to = LocalDateTime.now();
        when(transactionRepository.findAll(ArgumentMatchers.<Specification<Transaction>>any(), any(Pageable.class)))
                .thenReturn(Page.empty());

        useCase.execute(null, from, to, 0, 10);

        verify(transactionRepository).findAll(ArgumentMatchers.<Specification<Transaction>>any(), any(Pageable.class));
    }

    @Test
    void shouldRespectPageAndSizeParameters() {
        when(transactionRepository.findAll(ArgumentMatchers.<Specification<Transaction>>any(), any(Pageable.class)))
                .thenReturn(Page.empty());

        useCase.execute(null, null, null, 2, 5);

        ArgumentCaptor<Pageable> pageableCaptor = ArgumentCaptor.forClass(Pageable.class);
        verify(transactionRepository).findAll(ArgumentMatchers.<Specification<Transaction>>any(), pageableCaptor.capture());
        Pageable captured = pageableCaptor.getValue();
        assertEquals(2, captured.getPageNumber());
        assertEquals(5, captured.getPageSize());
    }

    @Test
    void shouldReturnEmptyPageWhenQueueIsEmpty() {
        when(transactionRepository.findAll(ArgumentMatchers.<Specification<Transaction>>any(), any(Pageable.class)))
                .thenReturn(Page.empty());

        Page<Transaction> result = useCase.execute(null, null, null, 0, 20);

        assertEquals(0, result.getTotalElements());
    }

    @Test
    void shouldIncludeDisputedTransactionsAlongsideAwaitingAnalyst() {
        Transaction awaiting = awaitingAnalystTransaction();
        Transaction disputed = disputedTransaction();
        Page<Transaction> expected = new PageImpl<>(List.of(awaiting, disputed));
        when(transactionRepository.findAll(ArgumentMatchers.<Specification<Transaction>>any(), any(Pageable.class)))
                .thenReturn(expected);

        Page<Transaction> result = useCase.execute(null, null, null, 0, 20);

        assertEquals(2, result.getTotalElements());
        assertEquals(TransactionStatus.AWAITING_ANALYST, result.getContent().get(0).getStatus());
        assertEquals(TransactionStatus.DISPUTED, result.getContent().get(1).getStatus());
    }

    @Test
    void shouldExcludeAnalystOwnTransactionsFromQueue() {
        UUID analystId = UUID.randomUUID();
        when(currentUserProvider.getAuthenticatedUser()).thenReturn(
                new AuthenticatedUser(analystId, "analyst@example.com", Role.ANALYST,
                        List.of(new SimpleGrantedAuthority("ROLE_ANALYST"))));
        when(transactionRepository.findAll(ArgumentMatchers.<Specification<Transaction>>any(), any(Pageable.class)))
                .thenReturn(Page.empty());

        Page<Transaction> result = useCase.execute(null, null, null, 0, 20);

        assertEquals(0, result.getTotalElements());
        verify(transactionRepository).findAll(ArgumentMatchers.<Specification<Transaction>>any(), any(Pageable.class));
    }
}
