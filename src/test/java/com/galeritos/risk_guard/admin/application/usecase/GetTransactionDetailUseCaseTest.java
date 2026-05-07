package com.galeritos.risk_guard.admin.application.usecase;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Optional;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import com.galeritos.risk_guard.banking.domain.exception.TransactionNotFoundException;
import com.galeritos.risk_guard.banking.domain.model.Transaction;
import com.galeritos.risk_guard.banking.domain.model.enums.FinancialStatus;
import com.galeritos.risk_guard.banking.domain.model.enums.TransactionStatus;
import com.galeritos.risk_guard.banking.infrastructure.persistence.repository.TransactionRepository;

@ExtendWith(MockitoExtension.class)
class GetTransactionDetailUseCaseTest {

    @Mock
    private TransactionRepository repository;

    private GetTransactionDetailUseCase useCase;

    @BeforeEach
    void setUp() {
        useCase = new GetTransactionDetailUseCase(repository);
    }

    @Test
    void shouldReturnTransactionWhenFound() {
        UUID transactionId = UUID.randomUUID();
        Transaction transaction = new Transaction(
                UUID.randomUUID(), UUID.randomUUID(), new BigDecimal("250.00"),
                TransactionStatus.APPROVED, FinancialStatus.SETTLED, null, LocalDateTime.now());
        ReflectionTestUtils.setField(transaction, "id", transactionId);

        when(repository.findById(transactionId)).thenReturn(Optional.of(transaction));

        Transaction result = useCase.execute(transactionId);

        assertEquals(transactionId, result.getId());
        assertEquals(TransactionStatus.APPROVED, result.getStatus());
    }

    @Test
    void shouldThrowWhenTransactionNotFound() {
        UUID transactionId = UUID.randomUUID();
        when(repository.findById(transactionId)).thenReturn(Optional.empty());

        assertThrows(TransactionNotFoundException.class, () -> useCase.execute(transactionId));
    }
}
